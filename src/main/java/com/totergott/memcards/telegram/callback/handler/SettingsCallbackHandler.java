package com.totergott.memcards.telegram.callback.handler;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.TextProvider;
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
    private final TextProvider textProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService client;
    CallbackSource callbackSource = CallbackSource.SETTINGS;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        SettingsCallback settingsCallback = (SettingsCallback) callback;
        Integer messageId = callbackQuery.getMessage().getMessageId();
        switch (settingsCallback.getAction()) {
            case LANGUAGE -> languageSettings(user, messageId);
            case CHANGE_LANGUAGE -> changeLanguage(settingsCallback, user, messageId);
            case INFO -> info();
        }
    }

    private void changeLanguage(SettingsCallback settingsCallback, TelegramUser user, Integer messageId) {
        var newLanguage = AvailableLocale.valueOf(settingsCallback.getData());
        changeDefaultCollectionsLanguage(user, newLanguage);
        user.setLanguage(newLanguage);
        var text = textProvider.getMessage("settings.language.updated", newLanguage, newLanguage.getReadableName());
        var keyboard = keyboardProvider.getMainMenu(user);

        client.sendMessage(text, keyboard);
        client.deleteMessage(user.getChatId(), messageId);
    }

    private void changeDefaultCollectionsLanguage(TelegramUser user, AvailableLocale newLanguage) {
        collectionService.getById(user.getPayload().getDefaultCollection()).ifPresent(defaultCollection -> {
            defaultCollection.setName(textProvider.getMessage(
                "default_collection_name", newLanguage));
        });

        // todo test default collection might not be present
        var tutorialCollectionId = user.getPayload().getTutorialCollectionId();
        if (tutorialCollectionId != null) {
            collectionService.getById(user.getPayload().getTutorialCollectionId()).ifPresent(
                tutorialCollection -> {
                    collectionService.deleteById(tutorialCollection.getId());
                    collectionService.initTutorialCollection(user, newLanguage);
                });
        }
    }

    private void languageSettings(TelegramUser user, Integer messageId) {
        var text = textProvider.getMessage("settings.language", user.getLanguage());
        var keyboard = keyboardProvider.buildLanguageKeyboard();
        client.editMessage(user.getChatId(), messageId, text, keyboard);
    }

    private void info() {
        client.deleteMessagesExceptFirst(1);
        client.sendMessage(textProvider.get("about_url"));
    }
}
