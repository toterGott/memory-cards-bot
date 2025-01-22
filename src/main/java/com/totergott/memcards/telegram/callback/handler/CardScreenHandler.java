package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getCallback;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.CommonHandler;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@RequiredArgsConstructor
@Slf4j

public class CardScreenHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService messageService;
    private final CommonHandler commonHandler;

    @Getter
    CallbackSource callbackSource = CallbackSource.CARD;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        CardCallback cardCallback = (CardCallback) callback;
        switch (cardCallback.getAction()) {
            case SHOW_ANSWER -> showAnswer(UUID.fromString(callback.getData()));

            case CHECK_INFO -> checkInfo();
            case CHECK_KNOWLEDGE -> checkKnowledge(UUID.fromString(callback.getData()), callback.getAdditionalData());

            case CONFIGS -> showConfigOptions(UUID.fromString(callback.getData()));
            case CHANGE_COLLECTION -> changeCollection(UUID.fromString(callback.getData()));
            case SET_COLLECTION -> setCollection(UUID.fromString(callback.getData()));
            case DELETE_DIALOG -> deleteCard(UUID.fromString(callback.getData()));
            case CANCEL_DELETE -> cancelDelete(UUID.fromString(callback.getData()));
            case CONFIRM_DELETE -> confirmDelete(UUID.fromString(callback.getData()));
            case NEXT_CARD -> nextCard();
            case BACK_TO_CARD -> backToCard(callback.getData());

            case EDIT -> editCard(callback.getData());
        }
    }

    public void showCard() {
        var user = getUser();
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(messageProvider.getText("no_cards"))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(
                    messageProvider.getText("no_cards_for_focus", user.getFocusedOnCollection().getName())
                )
            );
        }
    }

    private void showAnswer(UUID cardId) {
        var card = cardService.findById(cardId).orElseThrow();
        messageService.clearCallbackKeyboard();
        var answerKeyboard = keyboardProvider.getInlineKnowledgeCheckKeyboard(cardId);
        messageService.sendMessage(
            messageProvider.getText("emoji.answer") + card.getAnswer(),
            answerKeyboard
        );
    }

    private void checkInfo() {
        messageService.showAlert(
            getCallback().getId(),
            messageProvider.getText("knowledge_check_info")
        );
    }

    private void checkKnowledge(UUID uuid, String additionalData) {
        var card = cardService.findById(uuid).orElseThrow();
        var user = getUser();
        var grade = Integer.parseInt(additionalData);

        String text;
        Instant appearTime = Instant.now();
        int chronoUnitsAmount;
        ChronoUnit chronoUnit;

        switch (grade) {
            case 0 -> {
                chronoUnitsAmount = 10;
                chronoUnit = SECONDS;
            }
            case 1 -> {
                chronoUnitsAmount = 10;
                chronoUnit = MINUTES;
            }
            case 2 -> {
                chronoUnitsAmount = 1;
                chronoUnit = DAYS;
            }
            case 3 -> {
                chronoUnitsAmount = 4;
                chronoUnit = DAYS;
            }
            default -> {
                chronoUnitsAmount = 1;
                chronoUnit = SECONDS;
                log.error("Unknown grade: {}", grade);
            }
        }

        appearTime = appearTime.plus(chronoUnitsAmount, chronoUnit);
        card.setAppearTime(appearTime);
        user.setState(STAND_BY);

        text = messageProvider.getText(
            "card_answered",
            Integer.toString(chronoUnitsAmount), chronoUnit.toString().toLowerCase()
        );
        if (user.getFocusedOnCollection() != null) {
            var collectionName = collectionService.getById(user.getFocusedOnCollection().getId()).orElseThrow()
                .getName();
            text += " " + messageProvider.getMessage("collections.focus_on", user.getLanguage(), collectionName);
        }

        if (user.getPayload().getSchedule() != null) {
            var schedule = user.getPayload().getSchedule();
            var nextRun = schedule.getNextRun().plus(schedule.getHours(), HOURS);
            schedule.setNextRun(nextRun);
        }

        messageService.clearCallbackKeyboard();
        InlineKeyboardMarkup keyboard = keyboardProvider.getCardMenuAfterAnswer(card.getId());
        messageService.sendMessage(text, keyboard);
        // todo save last collection where the card was added and save next card in it
    }

    private void backToCard(String data) {
        var cardId = UUID.fromString(data);
        InlineKeyboardMarkup keyboard = keyboardProvider.getCardMenuAfterAnswer(cardId);
        var text = messageProvider.getText("card.actions");
        messageService.editCallbackKeyboard(keyboard);
    }

    private void nextCard() {
        showCard();
    }

    private void editCard(String data) {
        // todo introduce common component to edit card on creation, after answer and in cards browser
        var card = cardService.getCard(UUID.fromString(data));
        messageService.deleteMessagesExceptFirst(1);
        commonHandler.printCardWithEditButtons(card);
    }

    private void showConfigOptions(UUID uuid) {
        InlineKeyboardMarkup keyboard = keyboardProvider.getCardMenuAfterAnswerWithOptions(uuid);
        messageService.editCallbackKeyboard(keyboard);
    }

    private void selectCard(UUID cardId, String additionalData) {
        var card = cardService.findById(cardId).orElseThrow();
        var text = messageProvider.getText("collections.cards.selected", card.getQuestion(), card.getAnswer());
        var keyboard = keyboardProvider.buildCardKeyboard(cardId, additionalData);

        messageService.editCallbackMessage(text, keyboard);
    }

    private void setCollection(UUID id) {
        var user = getUser();
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(id).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }

        card.setCollection(collection);
        var text = messageProvider.getText("emoji.collection") +
            messageProvider.getText("card.collections.changed", collection.getName());
        var keyboard = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(text, keyboard);

        messageService.deleteCallbackMessage();

        user.setCurrentCardId(null);
    }

    private void changeCollection(UUID cardId) {
        getUser().setCurrentCardId(cardId);
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = messageProvider.getText(
            "card.collections.select",
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );

        // todho shouldn't be new card callback
        NewCardCallback pageCallback = NewCardCallback.builder()
            .action(NewCardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        text = messageProvider.appendPageInfo(text, page);
        messageService.editCallbackMessage(text, pageKeyboard);
    }

    private void cancelDelete(UUID cardId) {
        showConfigOptions(cardId);
    }

    private void confirmDelete(UUID cardId) {
        cardService.deleteById(cardId);

        messageService.editCallbackMessage(
            messageProvider.getText("card.delete.deleted"),
            keyboardProvider.getAfterCardDeleted()
        );
    }

    private void deleteCard(UUID cardId) {
        messageService.editCallbackMessage(
            messageProvider.getText("card.delete.confirm"),
            keyboardProvider.getCardDeleteConfirmation(cardId.toString())
        );
    }

//    private void back(UUID cardId, String pageNumber) {
//        var collectionId = cardService.findById(cardId).orElseThrow().getCollection().getId();
//        // todo dry, same code in CollectionHandler
//        var collectionName = collectionService.findById(collectionId).orElseThrow().getName();
//        var cardPage = cardService.getCardPageByCollectionId(collectionId, Integer.parseInt(pageNumber));
//        CardCallback cardCallback = new CardCallback();
//        cardCallback.setAction(CardCallbackAction.SELECT);
//        var keyboard = keyboardProvider.buildPage(cardPage, cardCallback);
//        var text = messageProvider.getText("collections.cards", collectionName);
//        text = messageProvider.appendPageInfo(text, cardPage);
//        messageService.editCallbackMessage(text, keyboard);
//    }

    private void sendCard(Card card, TelegramUser user) {
        messageService.sendMessage(messageProvider.getText("emoji.card"));

        var now = Instant.now();
        if (card.getAppearTime().isAfter(now)) {
            var diff = commonHandler.getDiff(now, card.getAppearTime());
            messageService.sendMessage(
                messageProvider.getText("no_cards_yet", diff),
                keyboardProvider.getBackToMainMenuReply()
            );
            messageService.deleteMessagesExceptLast(2);
            return;
        }
        var collectionName = card.getCollection().getName();
        messageService.sendMessage(
            messageProvider.getText("emoji.collection") + collectionName,
            keyboardProvider.getBackToMainMenuReply()
        );

        var keyboard = keyboardProvider.getInlineShowAnswerKeyboard(card.getId());
        messageService.sendMessage(messageProvider.getText("emoji.card") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(3);
    }

}
