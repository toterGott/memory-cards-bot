package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramCallbackHandler {

    private final TelegramClientWrapper client;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final CollectionService collectionService;
    private final CardService cardService;

    public void handleCallback(Update update, TelegramUser user) {
        var callbackArgs = update.getCallbackQuery().getData().split(CALLBACK_DELIMITER);
        var callbackAction = CallbackAction.valueOf(callbackArgs[0]);

        switch (callbackAction) {
            case SET_LANGUAGE -> handleLanguageChangeCallback(callbackArgs[1], user, update);
            case SELECT_COLLECTION -> handleSelectCollection(callbackArgs[1], user, update);
            case SELECT_COLLECTION_FOR_CARD -> handleCollectionSelectionForCard(
                callbackArgs[1],
                user,
                update
            );
            case FOCUS_ON_COLLECTION -> handleFocusOnCollection(callbackArgs[1], user, update);
            case SELECT_COLLECTION_PAGE -> handleCollectionPage(callbackArgs[1], user, update);
            case EDIT_COLLECTION_CARDS -> {
            }
            case DELETE_COLLECTION -> deleteCollection(callbackArgs[1], user, update);
            case CONFIRM_CARD_CREATION -> confirmCardCreation(user, update);
            case CHANGE_CARD_COLLECTION -> handleChangeCardCollection(callbackArgs[1], user, update);
        }

        var callbackId = update.getCallbackQuery().getId();
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        client.execute(answer);
    }

    private void deleteCollection(String collectionId, TelegramUser user, Update update) {
        var collectionsCount = collectionService.countUserCollections(user.getId());
        if (collectionsCount <= 1) {
            var callbackId = update.getCallbackQuery().getId();
            AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
            answer.setShowAlert(true);
            answer.setText(messageProvider.getMessage("collections.delete_error.last_collection", user.getLanguage()));
            client.execute(answer);
            return;
        }
        if (user.getFocusedOnCollection() != null
            && user.getFocusedOnCollection().getId().toString().equals(collectionId)) {
            user.setFocusedOnCollection(null);
        }

        collectionService.deleteById(collectionId);

        handleCollectionPage("0", user, update);
    }

    private void confirmCardCreation(TelegramUser user, Update update) {
        var keyboard = keyboardProvider.getMainMenu(user);
        var card = cardService.getCard(user.getCurrentCardId());
        var collectionName = card.getCollection().getName();
        var text = messageProvider.getMessage("create_card.created", user.getLanguage(), collectionName);
        client.sendMessage(user, text, keyboard);

        client.deleteMessage(user.getChatId(), update.getCallbackQuery().getMessage().getMessageId());

        user.setCurrentCardId(null);
    }

    private void handleCollectionSelectionForCard(
        String collectionId,
        TelegramUser user,
        Update update
    ) {
        var card = cardService.findById(user.getCurrentCardId()).orElseThrow();
        var collection = collectionService.findById(UUID.fromString(collectionId)).orElseThrow();
        if (card.getOwner().getId() != user.getId() || collection.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Access violation");
        }

        card.setCollection(collection);
        var text = messageProvider.getMessage("cards.collections.changed", user.getLanguage(), collection.getName());
        var keyboard = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, text, keyboard);

        client.deleteMessage(user.getChatId(), update.getCallbackQuery().getMessage().getMessageId());

        user.setCurrentCardId(null);
    }

    private void handleChangeCardCollection(String cardId, TelegramUser user, Update update) {
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = messageProvider.getMessage(
            "cards.collections.select",
            user.getLanguage(),
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );
        var pageKeyboard = keyboardProvider.getSelectCollectionForCardPage(user.getLanguage(), page);

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(pageKeyboard);
        client.execute(editMessageText);
    }

    private void handleFocusOnCollection(String collectionId, TelegramUser user, Update update) {
        var collection = collectionService.findById(UUID.fromString(collectionId)).orElseThrow();
        var text = messageProvider.getMessage("collections.focus_on", user.getLanguage(), collection.getName());
        user.setFocusedOnCollection(collection);
        var keyboard = keyboardProvider.getMainMenu(user);

        SendMessage sendMessage = new SendMessage(user.getChatId().toString(), text);
        sendMessage.setReplyMarkup(keyboard);
        client.execute(sendMessage);

        DeleteMessage deleteMessage = new DeleteMessage(
            user.getChatId().toString(),
            update.getCallbackQuery().getMessage().getMessageId()
        );
        client.execute(deleteMessage);
    }

    private void handleLanguageChangeCallback(String localeCode, TelegramUser user, Update update) {
        var locale = AvailableLocale.valueOf(localeCode);
        user.setLanguage(locale);

        var text = messageProvider.getMessage("language.updated", user.getLanguage(), locale.getName());
        client.sendMessage(user, text, keyboardProvider.getMainMenu(user));

        DeleteMessage deleteMessage = new DeleteMessage(
            user.getChatId().toString(),
            update.getCallbackQuery().getMessage().getMessageId()
        );
        client.execute(deleteMessage);
    }

    private void handleSelectCollection(String collectionId, TelegramUser user, Update update) {
        var collection = collectionService.findById(UUID.fromString(collectionId)).orElseThrow();
        var text = messageProvider.getMessage("collections.select", user.getLanguage(), collection.getName());
        var inlineKeyboard = keyboardProvider.getCollectionSelectedKeyboard(user.getLanguage(), collectionId);

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(inlineKeyboard);
        client.execute(editMessageText);
    }

    private void handleCollectionPage(String pageNumber, TelegramUser user, Update update) {
        var page = collectionService.getCollectionsPage(user.getId(), Integer.parseInt(pageNumber));
        var text = messageProvider.getMessage(
            "collections",
            user.getLanguage(),
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );
        var pageKeyboard = keyboardProvider.getCollectionsInlineKeyboardPage(user.getLanguage(), page);

        // todo make this cleaner
        if (user.getCurrentCardId() != null) {
            pageKeyboard = keyboardProvider.getSelectCollectionForCardPage(user.getLanguage(), page);
        }

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(pageKeyboard);
        client.execute(editMessageText);
    }
}
