package com.example.memcards.telegram.callback.handler;

import static com.example.memcards.telegram.TelegramUtils.getCallback;
import static com.example.memcards.telegram.TelegramUtils.getUser;
import static com.example.memcards.telegram.callback.CallbackMapper.readCallback;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.common.PageableEntity;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CardCallback;
import com.example.memcards.telegram.callback.model.NewCardCallback;
import com.example.memcards.telegram.callback.model.PageNavigationCallback;
import com.example.memcards.user.TelegramUser;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class PageNavigationCallbackHandler implements CallbackHandler {

    private final CollectionService collectionService;
    private final CardService cardService;
    private final MessageProvider messageProvider;
    private final KeyboardProvider keyboardProvider;
    private final TelegramClientWrapper client;
    CallbackSource callbackSource = CallbackSource.PAGE_NAVIGATION;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        var pageNavigationCallback = (PageNavigationCallback) callback;
        Message message = (Message) getCallback().getMessage();
        var pageCallback = readCallback(message.getReplyMarkup().getKeyboard().getFirst().getFirst().getCallbackData());
        var pageSource = pageCallback.getSource();
        var sourceKeyboard = message.getReplyMarkup();
        Page<? extends PageableEntity> newPage = null;
        String text = null;
        switch (pageSource) {
            case COLLECTIONS -> {
                // todo might be also extracted from source
                // page with create button
                text = messageProvider.getText("collections");
                newPage = collectionService.getCollectionsPage(
                    getUser().getId(),
                    Integer.parseInt(pageNavigationCallback.getData())
                );
            }
            case CARD -> {
                CardCallback cardCallback = (CardCallback) pageCallback;
                var collection = cardService.getCard(UUID.fromString(cardCallback.getData())).getCollection();
                text = messageProvider.getText("cards", collection.getName());
                newPage = cardService.getCardPageByCollectionId(
                    collection.getId(),
                    Integer.parseInt(pageNavigationCallback.getData())
                );
            }
            case NEW_CARD -> {
                text = messageProvider.getText("collections");
                newPage = collectionService.getCollectionsPage(
                    getUser().getId(),
                    Integer.parseInt(pageNavigationCallback.getData())
                );
            }
            default -> {
                client.showAlertNotImplemented();
                throw new RuntimeException("Not implemented");
            }
        }
        text = messageProvider.appendPageInfo(text, newPage);
        InlineKeyboardMarkup updatedKeyboard = keyboardProvider.updateKeyboard(sourceKeyboard, newPage);
        client.editCallbackMessage(text, updatedKeyboard);
    }
}
