package com.example.memcards.telegram.callback;

import static com.example.memcards.telegram.callback.CallbackMapper.readCallback;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramCallbackDelegate {

    private final TelegramClientWrapper client;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final CollectionService collectionService;
    private final CardService cardService;
    private final Set<CallbackHandler> callbackHandlers;
    private Map<CallbackSource, CallbackHandler> callbackHandlerMap;

    @PostConstruct
    public void init() {
        callbackHandlerMap = callbackHandlers.stream()
            .collect(Collectors.toMap(CallbackHandler::getCallbackSource, handler -> handler));
    }

    public void handleCallback(CallbackQuery callbackQuery, TelegramUser user) {
        Callback callback = readCallback(callbackQuery.getData());
        var handler = callbackHandlerMap.get(callback.getSource());
        if (handler == null) {
            throw new IllegalStateException("Unexpected action source value: " + callback.getSource());
        }
        handler.handle(callback, callbackQuery, user);

//        switch (callbackAction) {
//            case SET_LANGUAGE -> handleLanguageChangeCallback(callbackArgs[1], user, update);
//            case SELECT_COLLECTION_FOR_CARD -> handleCollectionSelectionForCard(
//                callbackArgs[1],
//                user,
//                update
//            );
//            case EDIT_COLLECTION_CARDS -> {
//            }
//            case CONFIRM_CARD_CREATION -> confirmCardCreation(user, update);
//            case CHANGE_CARD_COLLECTION -> handleChangeCardCollection(callbackArgs[1], user, update);
//        }

        var callbackId = callbackQuery.getId();
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        client.execute(answer);
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
        var pageKeyboard = keyboardProvider.buildCollectionPageForCardSelectionOnCreation(user.getLanguage(), page);

        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(pageKeyboard);
        client.execute(editMessageText);
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
}
