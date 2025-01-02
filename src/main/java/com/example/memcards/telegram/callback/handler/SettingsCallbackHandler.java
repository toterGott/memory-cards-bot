package com.example.memcards.telegram.callback.handler;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.SettingsCallback;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
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
    private final TelegramClientWrapper client;
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
