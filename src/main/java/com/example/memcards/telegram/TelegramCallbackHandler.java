package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;

import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class TelegramCallbackHandler {

    private final TelegramClientWrapper client;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final CollectionService collectionService;

    public void handleCallback(Update update, TelegramUser user) {
        var callbackArgs = update.getCallbackQuery().getData().split(CALLBACK_DELIMITER);
        var callbackAction = CallbackAction.valueOf(callbackArgs[0]);

        switch (callbackAction) {
            case SET_LANGUAGE -> handleLanguageChangeCallback(callbackArgs[1], user, update);
            case SELECT_COLLECTION -> handleSelectCollection(callbackArgs[1], user, update);
            case FOCUS_ON_COLLECTION -> handleFocusOnCollection(callbackArgs[1], user, update);
            case SELECT_COLLECTION_PAGE -> handleCollectionPage(callbackArgs[1], user, update);
            case EDIT_COLLECTION_CARDS -> {
            }
        }

        var callbackId = update.getCallbackQuery().getId();
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        client.execute(answer);
    }

    private void handleFocusOnCollection(String collectionId, TelegramUser user, Update update) {
        var collection = collectionService.findById(UUID.fromString(collectionId)).orElseThrow();
        var text = messageProvider.getMessage("collections.focus_on", user.getLanguage(), collection.getName());
        user.getPayload().setFocusOnCollection(collection.getId());
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

        client.sendMessage(
            user,
            messageProvider.getMessage(
                "language.updated",
                user.getLanguage(),
                locale.getName()
            ),
            keyboardProvider.getMainMenu(user)
        );

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
        editMessageText.enableMarkdown(true);
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
        var pageKeyboard = keyboardProvider.getCollectionsPageInlineKeyboard(user.getLanguage(), page);

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(pageKeyboard);
        client.execute(editMessageText);
    }
}
