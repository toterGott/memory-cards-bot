package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.TelegramClientWrapper;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CardCallback;
import com.totergott.memcards.telegram.callback.model.CardCallback.CardCallbackAction;
import com.totergott.memcards.user.TelegramUser;
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

public class CardCallbackHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final TelegramClientWrapper client;

    @Getter
    CallbackSource callbackSource = CallbackSource.CARD;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        CardCallback cardCallback = (CardCallback) callback;
        switch (cardCallback.getAction()) {
            case DELETE -> deleteCard(UUID.fromString(callback.getData()));
            case DELETE_CONFIRM -> confirmDelete(UUID.fromString(callback.getData()));
            case CHANGE_COLLECTION -> changeCollection(UUID.fromString(callback.getData()));
            case CANCEL -> cancel(UUID.fromString(callback.getData()));
            case SET_COLLECTION -> setCollection(UUID.fromString(callback.getData()));
            case CHANGE_PAGE -> changePage(callback.getData());
            case SELECT -> selectCard(UUID.fromString(callback.getData()), callback.getAdditionalData());
            case BACK -> back(UUID.fromString(callback.getData()), callback.getAdditionalData());
        }
    }

    private void selectCard(UUID cardId, String additionalData) {
        var card = cardService.findById(cardId).orElseThrow();
        var text = messageProvider.getText("card.selected", card.getQuestion(), card.getAnswer());
        var keyboard = keyboardProvider.buildCardKeyboard(cardId, additionalData);

        client.editCallbackMessage(text, keyboard);
    }

    private void setCollection(UUID id) {
        var user = getUser();
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(id).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }

        card.setCollection(collection);
        var text = messageProvider.getText("card.collections.changed", collection.getName());
        var keyboard = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, text, keyboard);

        client.deleteCallbackMessage();

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

        CardCallback pageCallback = CardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(CardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        text = messageProvider.appendPageInfo(text, page);
        client.editCallbackMessage(text, pageKeyboard);
    }

    private void changePage(String pageNumber) {
        var page = collectionService.getCollectionsPage(getUser().getId(), Integer.parseInt(pageNumber));
        var text = messageProvider.getText(
            "card.collections.select",
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );

        CardCallback pageCallback = CardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(CardCallbackAction.SET_COLLECTION)
            .build();

        var pageKeyboard = keyboardProvider.buildPage(
            page,
            pageCallback
        );

        text = messageProvider.appendPageInfo(text, page);
        client.editCallbackMessage(text, pageKeyboard);
    }

    private void cancel(UUID cardId) {
        InlineKeyboardMarkup keyboard = keyboardProvider.getAfterCardAnswer(cardId);
        var text = messageProvider.getText("card.actions");
        client.editCallbackMessage(text, keyboard);
    }

    private void confirmDelete(UUID cardId) {
        cardService.deleteById(cardId);

        client.editCallbackMessage(
            messageProvider.getText("card.delete.deleted"),
            null
        );
    }

    private void deleteCard(UUID cardId) {
        client.editCallbackMessage(
            messageProvider.getText("card.delete.confirm"),
            keyboardProvider.getCardDeleteConfirmation(cardId.toString())
        );
    }

    private void back(UUID cardId, String pageNumber) {
        var collectionId = cardService.findById(cardId).orElseThrow().getCollection().getId();
        // todo dry, same code in CollectionHandler
        var collectionName = collectionService.findById(collectionId).orElseThrow().getName();
        var cardPage = cardService.getCardPageByCollectionId(collectionId, Integer.parseInt(pageNumber));
        CardCallback cardCallback = new CardCallback();
        cardCallback.setAction(CardCallbackAction.SELECT);
        var keyboard = keyboardProvider.buildPage(cardPage, cardCallback);
        var text = messageProvider.getText("cards", collectionName);
        text = messageProvider.appendPageInfo(text, cardPage);
        client.editCallbackMessage(text, keyboard);
    }
}
