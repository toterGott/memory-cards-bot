package com.totergott.memcards.telegram.callback.handler;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.SettingsCallback;
import com.totergott.memcards.user.AvailableLocale;
import com.totergott.memcards.user.TelegramUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class SettingsCallbackHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService client;
    CallbackSource callbackSource = CallbackSource.SETTINGS;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        SettingsCallback settingsCallback = (SettingsCallback) callback;
        Integer messageId = callbackQuery.getMessage().getMessageId();
        switch (settingsCallback.getAction()) {
            case LANGUAGE -> languageSettings(settingsCallback, user, messageId);
            case CHANNEL_LANGUAGE -> changeLanguage(settingsCallback, user, messageId);
        }
    }

    private void changeLanguage(SettingsCallback settingsCallback, TelegramUser user, Integer messageId) {
        var newLanguage = AvailableLocale.valueOf(settingsCallback.getData());
        user.setLanguage(newLanguage);
        var text = messageProvider.getMessage("settings.language.updated", newLanguage, newLanguage.getReadableName());
        var keyboard = keyboardProvider.getMainMenu(user);

        client.sendMessage(user, text, keyboard);
        client.deleteMessage(user.getChatId(), messageId);
    }

    private void languageSettings(SettingsCallback settingsCallback, TelegramUser user, Integer messageId) {
        var text = messageProvider.getMessage("settings.language", user.getLanguage());
        var keyboard = keyboardProvider.buildLanguageKeyboard();
        client.editMessage(user.getChatId(), messageId, text, keyboard);
    }
}
