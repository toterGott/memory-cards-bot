package com.example.memcards.telegram.callback.handler;

import static com.example.memcards.telegram.TelegramUtils.getUser;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.NewCardCallback;
import com.example.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.example.memcards.user.TelegramUser;
import com.example.memcards.user.UserState;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Component
@RequiredArgsConstructor
@Slf4j

public class NewCardCallbackHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final TelegramClientWrapper client;

    @Getter
    CallbackSource callbackSource = CallbackSource.NEW_CARD;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        NewCardCallback newCardCallback = (NewCardCallback) callback;
        Integer messageId = callbackQuery.getMessage().getMessageId();
        switch (newCardCallback.getAction()) {
            case CONFIRM -> confirmCardCreation(user, messageId);
            case CHANGE_COLLECTION -> changeCardCollection(user, messageId);
            case SET_COLLECTION -> setCollection(newCardCallback.getData(), user, messageId);
            case CHANGE_PAGE -> changePage(callback.getData());
        }
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
        client.sendMessage(user, text, keyboard);

        client.deleteMessage(user.getChatId(), messageId);

        user.setCurrentCardId(null);
        user.setState(UserState.STAND_BY);
    }

    private void confirmCardCreation(TelegramUser user, Integer messageId) {
        var keyboard = keyboardProvider.getMainMenu(user);
        var card = cardService.getCard(user.getCurrentCardId());
        var collectionName = card.getCollection().getName();
        var text = messageProvider.getMessage("create_card.created", user.getLanguage(), collectionName);
        client.sendMessage(user, text, keyboard);

        client.deleteMessage(user.getChatId(), messageId);

        user.setCurrentCardId(null);
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
        client.editCallbackMessage(text, pageKeyboard);
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

        client.editMessage(user.getChatId(), messageId, text, pageKeyboard);
    }
}
