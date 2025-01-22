package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.WAIT_CARD_QUESTION_INPUT;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CardCollection;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.CommonHandler;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserState;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewCardActionsHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService messageService;
    private final CommonHandler commonHandler;

    @Getter
    CallbackSource callbackSource = CallbackSource.NEW_CARD;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        NewCardCallback newCardCallback = (NewCardCallback) callback;
        Integer messageId = callbackQuery.getMessage().getMessageId();
        switch (newCardCallback.getAction()) {
            case CHANGE_COLLECTION -> changeCardCollection(user, messageId);
            case SET_COLLECTION -> setCollection(newCardCallback.getData(), user, messageId);
            case CHANGE_PAGE -> changePage(callback.getData());
            case EDIT_QUESTION -> editQuestion(callback.getData());
            case EDIT_ANSWER -> editAnswer(callback.getData());
            case CONFIRM -> confirmCardCreation(UUID.fromString(callback.getData()));
        }
    }

    public void startCreateCardDialog() {
        getUser().setState(WAIT_CARD_QUESTION_INPUT);
        var cardId = getUser().getCurrentCardId();
        if (cardId != null) {
            cardService.deleteIfUnfinished(getUser().getCurrentCardId());
            getUser().setCurrentCardId(null);
        }
        messageService.sendMessage(messageProvider.getText("emoji.create"));
        var text = messageProvider.getText("create.card.question_prompt");
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
        commonHandler.printCardWithEditButtons(card);

        if (card.getAnswer() == null) {
            getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);
            var answerMessageText = messageProvider.getText("create.card.answer_prompt");
            messageService.sendMessage(
                answerMessageText,
                ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(answerMessageText).build()
            );
        }
    }

    private void editQuestion(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        getUser().setState(UserState.WAIT_CARD_QUESTION_INPUT);

        messageService.deleteMessagesExceptFirst(1);
        commonHandler.printCardWithEditButtons(card);

        var text = messageProvider.getText("create.card.question_prompt");
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
        commonHandler.printCardWithEditButtons(card);
    }

    private void editAnswer(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);

        messageService.deleteMessagesExceptFirst(1);
        commonHandler.printCardWithEditButtons(card);

        var text = messageProvider.getText("create.card.answer_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
    }

    private void confirmCardCreation(UUID cardId) {
        var card = cardService.getCard(cardId);
        var collectionName = card.getCollection().getName();
        messageService.checkoutMainMenu();

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

        commonHandler.printCardWithEditButtons(card);

        user.setCurrentCardId(null);
        user.setState(UserState.STAND_BY);
        user.getPayload().setLastChosenCollectionId(collectionId);
        user.getPayload().setLastChosenCollectionTimestamp(now());
    }

    private void changePage(String pageNumber) {
        var user = getUser();
        var page = collectionService.getCollectionsPage(user.getId(), Integer.parseInt(pageNumber));
        var text = messageProvider.getText("collections");

        NewCardCallback pageCallback = NewCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(NewCardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(page, pageCallback);

        text = messageProvider.appendPageInfo(text, page);
        messageService.editCallbackMessage(text, pageKeyboard);
    }

    private void changeCardCollection(TelegramUser user, Integer messageId) {
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = messageProvider.getText("card.collections.select");
        text = messageProvider.appendPageInfo(text, page);

        NewCardCallback pageCallback = NewCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(NewCardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        messageService.editMessage(user.getChatId(), messageId, text, pageKeyboard);
    }
}
