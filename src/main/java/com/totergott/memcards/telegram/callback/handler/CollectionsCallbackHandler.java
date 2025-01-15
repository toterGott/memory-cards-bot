package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getCallback;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.COLLECTION_CREATION;

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
import com.totergott.memcards.telegram.callback.model.CardCallback.CardCallbackAction;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class CollectionsCallbackHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService client;
    private final CommonHandler commonHandler;
    CallbackSource callbackSource = CallbackSource.COLLECTIONS;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        CollectionsCallback collectionsCallback = (CollectionsCallback) callback;
        var messageId = callbackQuery.getMessage().getMessageId();
        switch (collectionsCallback.getAction()) {
            case SELECT -> handleSelectCollection(
                UUID.fromString(collectionsCallback.getData()),
                callback.getAdditionalData(),
                messageId,
                user
            );
            case FOCUS_ON_COLLECTION -> handleFocusOnCollection(
                UUID.fromString(collectionsCallback.getData()),
                messageId,
                user
            );
            case BACK -> handleCollectionPage(callback.getAdditionalData(), user, messageId);
            case NEW_COLLECTION -> createCollection();
            case CHANGE_PAGE -> handleCollectionPage(
                collectionsCallback.getData(),
                user,
                messageId
            );
            case EDIT_CARDS -> cardsPage(UUID.fromString(collectionsCallback.getData()));
            case DELETE -> deleteCollection(
                UUID.fromString(collectionsCallback.getData()),
                messageId,
                user,
                callbackQuery.getId()
            );
            case CONFIRM_DELETE -> confirmDelete(
                UUID.fromString(collectionsCallback.getData())
            );
        }
    }

    private void cardsPage(UUID collectionId) {
        var collectionName = collectionService.findById(collectionId).orElseThrow().getName();
        var cardPage = cardService.getCardPageByCollectionId(collectionId, 0);
        CardCallback cardCallback = new CardCallback();
        cardCallback.setAction(CardCallbackAction.SELECT);
        var keyboard = keyboardProvider.buildPage(cardPage, cardCallback);
        var text = messageProvider.getText("collections.cards", collectionName);
        text = messageProvider.appendPageInfo(text, cardPage);
        client.editCallbackMessage(text, keyboard);
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        var text = messageProvider.getText("create.collection.name_prompt");
        client.deleteCallbackMessage();
        client.sendMessage(text, new ReplyKeyboardRemove(true));
    }

    private void confirmDelete(UUID collectionId) {
        var user = getUser();
        if (user.getFocusedOnCollection() != null
            && user.getFocusedOnCollection().getId().equals(collectionId)) {
            user.setFocusedOnCollection(null);
        }
        var collectionName = collectionService.findById(collectionId).orElseThrow().getName();
        collectionService.deleteById(collectionId);

        var text = messageProvider.getText("collections.deleted", collectionName);
        client.showAlert(getCallback().getId(), text);
        commonHandler.collectionsScreen();
    }

    private void handleSelectCollection(UUID collectionId, String pageNumber, Integer messageId, TelegramUser user) {
        var collection = collectionService.findById(collectionId).orElseThrow();
        var text = messageProvider.getMessage("collections.selected", user.getLanguage(), collection.getName());
        var inlineKeyboard = keyboardProvider.buildCollectionSelectedOptionsKeyboard(user.getLanguage(), collectionId
            , pageNumber);

        client.editMessage(user.getChatId(), messageId, text, inlineKeyboard);
    }

    private void handleFocusOnCollection(UUID collectionId, Integer messageId, TelegramUser user) {
        var collection = collectionService.findById(collectionId).orElseThrow();
        var text = messageProvider.getMessage("collections.focus_on", user.getLanguage(), collection.getName());
        user.setFocusedOnCollection(collection);
        var keyboard = keyboardProvider.getMainMenu(user);

        client.sendMessage( text, keyboard);
        client.deleteMessage(user.getChatId(), messageId);
    }

    private void deleteCollection(UUID collectionId, Integer messageId, TelegramUser user, String callbackId) {
        if (Objects.equals(collectionId, user.getPayload().getDefaultCollection())) {
            var text = messageProvider.getMessage(
                "collections.delete_error.default_collection",
                user.getLanguage()
            );
            client.showAlert(callbackId, text);
            return;
        }

        var collection = collectionService.findById(collectionId).orElseThrow();
        var text = messageProvider.getMessage(
            "collections.delete.confirmation",
            user.getLanguage(),
            collection.getName()
        );
        var keyboard = keyboardProvider.buildDeleteConfirmationDialog(user, collectionId);
        client.editMessage(user.getChatId(), messageId, text, keyboard);
    }

    private void handleCollectionPage(String pageNumber, TelegramUser user, Integer messageId) {
        var page = collectionService.getCollectionsPage(user.getId(), Integer.parseInt(pageNumber));
        var text = messageProvider.getMessage(
            "collections",
            user.getLanguage()
        );
        CollectionsCallback pageItemCallback = new CollectionsCallback();
        pageItemCallback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = keyboardProvider.buildPage(page, pageItemCallback);

        text = messageProvider.appendPageInfo(text, page);
        client.editMessage(user.getChatId(), messageId, text, pageKeyboard);
    }
}
