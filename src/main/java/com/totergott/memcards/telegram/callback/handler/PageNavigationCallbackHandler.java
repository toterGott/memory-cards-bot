package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getCallback;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.CallbackMapper.readCallback;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.common.PageableEntity;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.GetCardCallback;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback;
import com.totergott.memcards.user.TelegramUser;
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
    private final MessageService client;
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
            case GET_CARD -> {
                GetCardCallback getCardCallback = (GetCardCallback) pageCallback;
                var collection = cardService.getCard(UUID.fromString(getCardCallback.getData())).getCollection();
                text = messageProvider.getText("collections.cards", collection.getName());
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
                client.notImplementedAlert();
                throw new RuntimeException("Not implemented");
            }
        }
        text = messageProvider.appendPageInfo(text, newPage);
        InlineKeyboardMarkup updatedKeyboard = keyboardProvider.updateKeyboard(sourceKeyboard, newPage);
        client.editCallbackMessage(text, updatedKeyboard);
    }
}
