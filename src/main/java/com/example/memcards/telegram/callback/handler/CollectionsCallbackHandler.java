package com.example.memcards.telegram.callback.handler;

import static com.example.memcards.telegram.TelegramUtils.getCallbackMessageId;
import static com.example.memcards.telegram.TelegramUtils.getChatId;
import static com.example.memcards.telegram.TelegramUtils.getUser;
import static com.example.memcards.user.UserState.COLLECTION_CREATION;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CollectionsCallback;
import com.example.memcards.user.TelegramUser;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
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
    private final TelegramClientWrapper client;
    CallbackSource callbackSource = CallbackSource.COLLECTIONS;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        CollectionsCallback collectionsCallback = (CollectionsCallback) callback;
        var messageId = callbackQuery.getMessage().getMessageId();
        switch (collectionsCallback.getAction()) {
            case SELECT -> handleSelectCollection(
                UUID.fromString(collectionsCallback.getData()),
                messageId,
                user
            );
            case FOCUS_ON_COLLECTION -> handleFocusOnCollection(
                UUID.fromString(collectionsCallback.getData()),
                messageId,
                user
            );
            case BACK -> handleCollectionPage("0", user, messageId);
            case NEW_COLLECTION -> createCollection();
            case CHANGE_PAGE -> handleCollectionPage(
                collectionsCallback.getData(),
                user,
                messageId
            );
            case EDIT_CARDS -> client.showAlert(
                callbackQuery.getId(),
                "UNDER DEVELOPMENT"
            );
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

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        var text = messageProvider.getText("collections.create");
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

        client.deleteMessage(getChatId(), getCallbackMessageId());
        var menu = keyboardProvider.getMainMenu(user);
        var text = messageProvider.getText("collections.deleted", collectionName);
        client.sendMessage(text, menu);
    }

    private void handleSelectCollection(UUID collectionId, Integer messageId, TelegramUser user) {
        var collection = collectionService.findById(collectionId).orElseThrow();
        var text = messageProvider.getMessage("collections.select", user.getLanguage(), collection.getName());
        var inlineKeyboard = keyboardProvider.buildCollectionSelectedOptionsKeyboard(user.getLanguage(), collectionId);

        client.editMessage(user.getChatId(), messageId, text, inlineKeyboard);
    }

    private void handleFocusOnCollection(UUID collectionId, Integer messageId, TelegramUser user) {
        var collection = collectionService.findById(collectionId).orElseThrow();
        var text = messageProvider.getMessage("collections.focus_on", user.getLanguage(), collection.getName());
        user.setFocusedOnCollection(collection);
        var keyboard = keyboardProvider.getMainMenu(user);

        client.sendMessage(user, text, keyboard);
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
            user.getLanguage(),
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );
        var pageKeyboard = keyboardProvider.buildCollectionsPage(page);

//        // todo make this cleaner
//        if (user.getCurrentCardId() != null) {
//            pageKeyboard = keyboardProvider.buildCollectionPageForCardSelectionOnCreation(user.getLanguage(), page);
//        }

        client.editMessage(user.getChatId(), messageId, text, pageKeyboard);
    }

    private void handleBackToPage(String cardId, TelegramUser user, Update update) {
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = messageProvider.getMessage(
            "card.collections.select",
            user.getLanguage(),
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );
        var pageKeyboard = keyboardProvider.buildCollectionsPage(page);

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(pageKeyboard);
        client.execute(editMessageText);
    }
}
