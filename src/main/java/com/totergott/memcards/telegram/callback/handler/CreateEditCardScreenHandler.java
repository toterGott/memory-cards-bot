package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getCallback;
import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.CANCEL_DELETE;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.CONFIRM_DELETE;
import static com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction.SET_COLLECTION;
import static com.totergott.memcards.user.UserState.WAIT_CARD_QUESTION_INPUT;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CardCollection;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.InlineKeyboardBuilder;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserState;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

@Component
@Slf4j
public class CreateEditCardScreenHandler extends CardHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;

    @Getter
    CallbackSource callbackSource = CallbackSource.NEW_CARD;

    public CreateEditCardScreenHandler(
        TextProvider textProvider,
        MessageService messageService,
        KeyboardProvider keyboardProvider,
        CollectionService collectionService,
        CardService cardService
    ) {
        super(textProvider, messageService, keyboardProvider);
        this.collectionService = collectionService;
        this.cardService = cardService;
    }

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        CreateEditCardCallback createEditCardCallback = (CreateEditCardCallback) callback;
        Integer messageId = callbackQuery.getMessage().getMessageId();
        switch (createEditCardCallback.getAction()) {
            // todo these buttons also should have breadcrumbs
            case EDIT_COLLECTION -> changeCardCollection(createEditCardCallback.getData());
            case SET_COLLECTION -> setCollection(createEditCardCallback.getData(), user, messageId);
            case CHANGE_PAGE -> changePage(callback.getData());
            case EDIT_QUESTION -> editQuestion(callback.getData());
            case EDIT_ANSWER -> editAnswer(callback.getData());

            case CONFIRM -> confirmCardCreation();
            case DELETE_DIALOG -> deleteCardDialog(UUID.fromString(callback.getData()));
            case CANCEL_DELETE -> cancelDelete(UUID.fromString(callback.getData()));
            case CONFIRM_DELETE -> confirmDelete(UUID.fromString(callback.getData()));
        }
    }

    public void startCreateCardDialog() {
        getUser().setState(WAIT_CARD_QUESTION_INPUT);
        var cardId = getUser().getCurrentCardId();
        if (cardId != null) {
            cardService.deleteIfUnfinished(getUser().getCurrentCardId());
            getUser().setCurrentCardId(null);
        }
        messageService.sendMessage(textProvider.get("emoji.create"));
        var text = textProvider.get("create.card.question_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
        messageService.deleteMessagesExceptLast(2);
    }

    public void handleQuestionInput() {
        var question = getMessage().getText();
        var user = getUser();
        Card card;

        if (getUser().getCurrentCardId() == null) {
            card = new Card();
            card.setQuestion(question);
            card.setOwner(user);
            card.setAppearTime(now());
            card = cardService.save(card);
            user.setCurrentCardId(card.getId());
        } else {
            card = cardService.getCard(getUser().getCurrentCardId());
            card.setQuestion(question);
        }

        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );

        if (card.getAnswer() == null) {
            getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);
            var answerMessageText = textProvider.get("create.card.answer_prompt");
            messageService.sendMessage(
                answerMessageText,
                ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(answerMessageText).build()
            );
        }
    }

    private void editQuestion(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        getUser().setState(UserState.WAIT_CARD_QUESTION_INPUT);
        getUser().setCurrentCardId(card.getId());

        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );

        var text = textProvider.get("create.card.question_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );

    }

    public void handleAnswerInput() {
        var answer = getMessage().getText();
        getUser().setState(UserState.STAND_BY);
        Card card = cardService.getCard(getUser().getCurrentCardId());
        card.setAnswer(answer);

        UUID collectionId;
        var payload = getUser().getPayload();
        if (payload.getLastChosenCollectionId() != null
            && payload.getLastChosenCollectionTimestamp().isAfter(now().minus(1, MINUTES))) {
            collectionId = payload.getLastChosenCollectionId();
            payload.setLastChosenCollectionTimestamp(now());
        } else {
            collectionId = payload.getDefaultCollection();
        }
        CardCollection collection = collectionService.getById(collectionId).orElseThrow();
        card.setCollection(collection);

        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );
    }

    private void editAnswer(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);
        getUser().setCurrentCardId(card.getId());

        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );

        var text = textProvider.get("create.card.answer_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
    }

    private void confirmCardCreation() {
        messageService.checkoutMainMenu();

        getUser().setState(UserState.STAND_BY);
        getUser().setCurrentCardId(null);
    }

    private void setCollection(
        String rawCollectionId,
        TelegramUser user,
        Integer messageId
    ) {
        UUID collectionId = UUID.fromString(rawCollectionId);
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(collectionId).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }
        card.setCollection(collection);

        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );

        user.setState(UserState.STAND_BY);
        user.getPayload().setLastChosenCollectionId(collectionId);
        user.getPayload().setLastChosenCollectionTimestamp(now());
    }

    private void changePage(String pageNumber) {
        var user = getUser();
        var page = collectionService.getCollectionsPage(user.getId(), Integer.parseInt(pageNumber));
        var text = textProvider.get("collections");

        CreateEditCardCallback pageCallback = CreateEditCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(page, pageCallback);

        text = textProvider.appendPageInfo(text, page);
        messageService.editCallbackMessage(text, pageKeyboard);
    }

    private void changeCardCollection(String cardId) {
        var messageId = getCallback().getMessage().getMessageId();
        var user = getUser();
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = textProvider.get("card.collections.select");
        text = textProvider.appendPageInfo(text, page);
        getUser().setCurrentCardId(UUID.fromString(cardId));

        CreateEditCardCallback pageCallback = CreateEditCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        messageService.editMessage(user.getChatId(), messageId, text, pageKeyboard);
    }

    private void cancelDelete(UUID cardId) {
        var card = cardService.findById(cardId).orElseThrow();
        messageService.deleteMessagesExceptFirst(1);
        printCardWithEditButtons(
            card,
            CreateEditCardCallback.builder().data(card.getId().toString()).action(CreateEditCardCallbackAction.CONFIRM)
                .build()
        );
    }

    private void confirmDelete(UUID cardId) {
        cardService.deleteById(cardId);

        var keyboardBuilder = new InlineKeyboardBuilder()
            .addButton(
                textProvider.get("button.back_to_main_menu"),
                CreateEditCardCallback.builder().action(CreateEditCardCallbackAction.CONFIRM).build()
            )
            .addRow();

//        switch (callbackSource) {
//            case GET_CARD -> keyboardBuilder.addButton(
//                textProvider.get("emoji.card")
//                    + textProvider.get("card.get_another"),
//                GetCardCallback.builder().action(GetCardCallbackAction.NEXT_CARD).build()
//            );
//        }

        messageService.deleteMessagesExceptFirst(1);
        messageService.sendMessage(
            textProvider.get("card.delete.deleted"),
            keyboardBuilder.build()
        );
    }

    private void deleteCardDialog(UUID cardId) {
        var keyboard = new InlineKeyboardBuilder()
            .addButton(
                textProvider.get("emoji.delete")
                    + textProvider.get("button.delete"),
                CreateEditCardCallback.builder()
                    .action(CONFIRM_DELETE)
                    .data(cardId.toString())
                    .build()
            )
            .addRow()
            .addButton(
                textProvider.get("emoji.back")
                    + textProvider.get("button.card.cancel"),
                CreateEditCardCallback.builder().
                    action(CANCEL_DELETE)
                    .data(cardId.toString())
                    .build()
            )
            .build();

        messageService.editCallbackMessage(textProvider.get("card.delete.confirmation_dialog"), keyboard);
    }
}
