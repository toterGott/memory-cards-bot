package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction.EDIT_ANSWER;
import static com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction.EDIT_QUESTION;
import static com.totergott.memcards.user.UserState.WAIT_CARD_QUESTION_INPUT;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CardCollection;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserService;
import com.totergott.memcards.user.UserState;
import java.time.Instant;
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
    private final UserService userService;

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
        messageService.sendMessage(messageProvider.getText("button.create.emoji"));
        var text = messageProvider.getText("create_card.question_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
        messageService.deleteMessagesExceptLast(2);
    }

    public void handleQuestionInput() {
        var question = getMessage().getText();
        var user = getUser();
        CardCollection collection = user.getFocusedOnCollection();
        if (collection == null) {
            collection = collectionService.getById(user.getPayload().getDefaultCollection()).orElseThrow();
        }
        Card card;

        if (getUser().getCurrentCardId() == null) {
            card = new Card();
            card.setQuestion(question);
            card.setCollection(collection);
            card.setOwner(user);
            card.setAppearTime(Instant.now());
            card = cardService.save(card);
            user.setCurrentCardId(card.getId());
        } else {
            card = cardService.getCard(getUser().getCurrentCardId());
            card.setQuestion(question);
        }

        messageService.deleteMessagesExceptFirst(1);
        printCard(card);

        if (card.getAnswer() == null) {
            getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);
            var answerMessageText = messageProvider.getText("create_card.answer_prompt");
            messageService.sendMessage(
                answerMessageText,
                ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(answerMessageText).build()
            );
        }
    }

    private void editQuestion(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        card.setQuestion(null);
        getUser().setState(UserState.WAIT_CARD_QUESTION_INPUT);

        messageService.deleteMessagesExceptFirst(1);
        printCard(card);

        var text = messageProvider.getText("create_card.question_prompt");
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

        messageService.deleteMessagesExceptFirst(1);
        printCard(card);
    }

    private void editAnswer(String data) {
        var card = cardService.getCard(UUID.fromString(data));
        card.setAnswer(null);
        getUser().setState(UserState.WAIT_CARD_ANSWER_INPUT);

        messageService.deleteMessagesExceptFirst(1);
        printCard(card);

        var text = messageProvider.getText("create_card.answer_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
    }

    private void confirmCardCreation(UUID cardId) {
        var keyboard = keyboardProvider.getMainMenu();
        var card = cardService.getCard(cardId);
        var collectionName = card.getCollection().getName();
        var text = messageProvider.getText("main_menu", collectionName);
        messageService.sendMessage(text, keyboard);
        messageService.deleteMessagesExceptLast(1);

        getUser().setCurrentCardId(null);
    }

    private void setCollection(
        String collectionId,
        TelegramUser user,
        Integer messageId
    ) {
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(UUID.fromString(collectionId)).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }

        card.setCollection(collection);
        var text = messageProvider.getMessage("card.collections.changed", user.getLanguage(), collection.getName());
        var keyboard = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(text, keyboard);

        messageService.deleteMessage(user.getChatId(), messageId);

        user.setCurrentCardId(null);
        user.setState(UserState.STAND_BY);
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

    private void printCard(Card card) {
        if (card.getQuestion() != null) {
            messageService.sendMessage(messageProvider.getText("create_card.question"));

            var buttonText = messageProvider.getText("button.card.edit_question");
            var callback = NewCardCallback.builder().action(EDIT_QUESTION).data(card.getId().toString()).build();
            messageService.sendMessage(card.getQuestion(), keyboardProvider.getOneInlineButton(buttonText, callback));
        }
        if (card.getAnswer() != null) {
            messageService.sendMessage(
                messageProvider.getText("create_card.answer"),
                keyboardProvider.getMainMenu()
            );

            var buttonText = messageProvider.getText("button.card.edit_answer");
            var callback = NewCardCallback.builder().action(EDIT_ANSWER).data(card.getId().toString()).build();
            messageService.sendMessage(card.getAnswer(), keyboardProvider.getOneInlineButton(buttonText, callback));
        }
        if (card.getQuestion() != null && card.getAnswer() != null) {
            printFinalMessage(card);
        }
    }

    private void printFinalMessage(Card card) {
        var cardCreatedText = messageProvider.getText(
            "create_card.created",
            card.getCollection().getName()
        );
        var cardCreatedInlineKeyboard = keyboardProvider.getCardCreatedInlineKeyboard(card.getId());
        messageService.sendMessage(cardCreatedText, cardCreatedInlineKeyboard);
    }
}
