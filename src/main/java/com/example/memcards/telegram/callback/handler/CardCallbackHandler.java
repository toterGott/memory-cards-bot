package com.example.memcards.telegram.callback.handler;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CardCallback;
import com.example.memcards.user.TelegramUser;
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
            case DELETE -> deleteCard(cardCallback.getData());
            case DELETE_CONFIRM -> confirmDelete(cardCallback.getData());
            case CHANGE_COLLECTION -> client.showAlertNotImplemented();
            case CANCEL -> cancel(cardCallback.getData());
        }
    }

    private void cancel(String cardId) {
        InlineKeyboardMarkup keyboard = keyboardProvider.getAfterCardAnswer(UUID.fromString(cardId));
        var text = messageProvider.getText("card.actions");
        client.editMessage(text, keyboard);
    }

    private void confirmDelete(String cardId) {
        cardService.deleteById(UUID.fromString(cardId));

        client.editMessage(
            messageProvider.getText("card.delete.deleted"),
            null
        );
    }

    private void deleteCard(String cardId) {
        client.editMessage(
            messageProvider.getText("card.delete.confirm"),
            keyboardProvider.getCardDeleteConfirmation(cardId)
        );
    }
}
