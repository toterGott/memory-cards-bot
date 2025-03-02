package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction.AFTER_ANSWER_INFO;
import static com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction.ARCHIVE;
import static com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction.EDIT;
import static com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction.EXTRACT;
import static com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction.NEXT_CARD;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.CommonHandler;
import com.totergott.memcards.telegram.InlineKeyboardBuilder;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.GetCardCallback;
import com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@Slf4j
public class GetCardHandler extends CardHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final CommonHandler commonHandler;

    @Getter
    CallbackSource callbackSource = CallbackSource.GET_CARD;

    public GetCardHandler(
        TextProvider textProvider,
        MessageService messageService,
        KeyboardProvider keyboardProvider,
        CollectionService collectionService,
        CardService cardService,
        CommonHandler commonHandler
    ) {
        super(textProvider, messageService, keyboardProvider);
        this.collectionService = collectionService;
        this.cardService = cardService;
        this.commonHandler = commonHandler;
    }

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        GetCardCallback getCardCallback = (GetCardCallback) callback;
        switch (getCardCallback.getAction()) {
            case SHOW_ANSWER -> showAnswer(UUID.fromString(callback.getData()));

            case CHECK_INFO -> checkInfoAlert();
            case CHECK_KNOWLEDGE -> checkKnowledge(UUID.fromString(callback.getData()), callback.getAdditionalData());

            case CHOOSE_ANOTHER_COLLECTION -> chooseAnotherCollection(UUID.fromString(callback.getData()));
            case SET_COLLECTION -> setCollection(UUID.fromString(callback.getData()));
            case NEXT_CARD -> nextCard();
            case OK_AFTER_EDIT -> commonHandler.setMainMenu();
            case ARCHIVE -> archive(UUID.fromString(callback.getData()));
            case EXTRACT -> extract(UUID.fromString(callback.getData()));

            case EDIT -> editCard(callback.getData());

            case AFTER_ANSWER_INFO -> infoAlertAfterAnswer(getCardCallback.getData());
            case SELECT -> editCollectionCard(getCardCallback.getData());
        }
    }

    private void editCollectionCard(String data) {
        messageService.deleteMessagesExceptFirst(1);
        var card = cardService.getCard(UUID.fromString(data));
        printCardWithEditButtons(
            card,
            CollectionsCallback.builder().action(CollectionCallbackAction.BROWSE_CARDS)
                .data(card.getCollection().getId().toString()).build()
        );
    }

    public void showCard() {
        var user = getUser();
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(textProvider.get("no_cards"))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(
                    textProvider.get("no_cards_for_focus", user.getFocusedOnCollection().getName())
                )
            );
        }
    }

    private void showAnswer(UUID cardId) {
        var card = cardService.findById(cardId).orElseThrow();
        messageService.clearCallbackKeyboard();
        var answerKeyboard = keyboardProvider.getInlineKnowledgeCheckKeyboard(cardId);
        messageService.sendMessage(
            textProvider.get("emoji.answer") + card.getAnswer(),
            answerKeyboard
        );
    }

    private void checkInfoAlert() {
        messageService.showCallbackAlert(textProvider.get("knowledge_check_info"));
    }

    private void infoAlertAfterAnswer(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        messageService.showCallbackAlert(textProvider.get(
            "card.info_after_answer",
            commonHandler.getDiff(card.getAppearTime())
        ));
    }

    private void checkKnowledge(UUID uuid, String additionalData) {
        var card = cardService.findById(uuid).orElseThrow();
        var user = getUser();
        var grade = Integer.parseInt(additionalData);


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

        if (user.getPayload().getSchedule() != null) {
            var schedule = user.getPayload().getSchedule();
            var option = schedule.getOption();
            var nextRun = Instant.now().plus(option.amount(), option.chronoUnit()); // todo add a test
            schedule.setNextRun(nextRun);
        }

        messageService.editCallbackKeyboard(getFinalKeyboard(card));
    }

    private void archive(UUID uuid) {
        var card = cardService.findById(uuid).orElseThrow();
        card.setArchived(true);
        messageService.editCallbackKeyboard(getFinalKeyboard(card));
    }

    private void extract(UUID uuid) {
        var card = cardService.findById(uuid).orElseThrow();
        card.setArchived(false);
        messageService.editCallbackKeyboard(getFinalKeyboard(card));
    }

    private InlineKeyboardMarkup getFinalKeyboard(Card card) {
        String text = textProvider.get(
            "card_answered",
            commonHandler.getDiff(card.getAppearTime())
        );

        String archiveText;
        Callback arcvhiveCallback;
        if (Boolean.TRUE.equals(card.getArchived())) {
            archiveText = textProvider.get("emoji.extract") + textProvider.get("extract");
            arcvhiveCallback = GetCardCallback.builder()
                .action(EXTRACT)
                .data(card.getId().toString())
                .build();
        } else {
            archiveText = textProvider.get("emoji.archive") + textProvider.get("archive");
            arcvhiveCallback = GetCardCallback.builder()
                .action(ARCHIVE)
                .data(card.getId().toString())
                .build();
        }

        return new InlineKeyboardBuilder()
            .addButton(text, GetCardCallback.builder().action(AFTER_ANSWER_INFO).data(card.getId().toString()).build())
            .nextRow()
            .addButton(
                textProvider.get("emoji.edit")
                + textProvider.get("button.edit"),
                GetCardCallback.builder()
                    .action(EDIT)
                    .data(card.getId().toString())
                    .build()
            )
            .nextRow()
            .addButton(archiveText, arcvhiveCallback)
            .nextRow()
            .addButton(
                textProvider.get("emoji.card")
                + textProvider.get("card.get_another"),
                GetCardCallback.builder()
                    .action(NEXT_CARD)
                    .data(card.getId().toString())
                    .build()
            )
            .build();
    }

    private void nextCard() {
        showCard();
    }

    private void editCard(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            GetCardCallback.builder().action(GetCardCallbackAction.OK_AFTER_EDIT).data(data).build()
        );
    }

    private void chooseAnotherCollection(UUID cardId) {
        getUser().setCurrentCardId(cardId);
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = textProvider.get(
            "card.collections.select",
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );

        GetCardCallback pageCallback = GetCardCallback.builder()
            .action(GetCardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        text = textProvider.appendPageInfo(text, page);
        messageService.editCallbackMessage(text, pageKeyboard);
    }

    private void setCollection(UUID id) {
        var user = getUser();
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(id).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }

        card.setCollection(collection);
        var text = textProvider.get("emoji.collection") +
            textProvider.get("card.collections.changed", collection.getName());
        var keyboard = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(text, keyboard);

        messageService.deleteCallbackMessage();

        user.setCurrentCardId(null);
    }

    public void sendCard(Card card, TelegramUser user) {
        messageService.sendMessage(textProvider.get("emoji.card"));

        var now = Instant.now();
        if (card.getAppearTime().isAfter(now)) {
            var diff = commonHandler.getDiff(card.getAppearTime());
            messageService.sendMessage(
                textProvider.get("no_cards_yet", diff),
                keyboardProvider.getBackToMainMenuReply()
            );
            messageService.deleteMessagesExceptLast(2);
            return;
        }
        var collectionName = card.getCollection().getName();
        messageService.sendMessage(
            textProvider.get("emoji.collection") + collectionName,
            keyboardProvider.getBackToMainMenuReply()
        );

        var keyboard = keyboardProvider.getInlineShowAnswerKeyboard(card.getId());
        messageService.sendMessage(textProvider.get("emoji.card") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(3);
    }

}
