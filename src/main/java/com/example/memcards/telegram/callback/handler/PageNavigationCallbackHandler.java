package com.example.memcards.telegram.callback.handler;

import static com.example.memcards.telegram.TelegramUtils.getCallback;
import static com.example.memcards.telegram.TelegramUtils.getChatId;
import static com.example.memcards.telegram.TelegramUtils.getUpdate;
import static com.example.memcards.telegram.TelegramUtils.getUser;
import static com.example.memcards.telegram.callback.CallbackMapper.readCallback;

import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.telegram.callback.CallbackHandler;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.PageNavigationCallback;
import com.example.memcards.telegram.callback.model.SettingsCallback;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        Message message =  (Message) getCallback().getMessage();
        var pageCallback = readCallback(message.getReplyMarkup().getKeyboard().getFirst().getFirst().getCallbackData());
        var pageSource = pageCallback.getSource();
        var sourceKeyboard = message.getReplyMarkup();
        var newPage = collectionService.getCollectionsPage(getUser().getId(),
                                                  Integer.parseInt(pageNavigationCallback.getData()));
        InlineKeyboardMarkup updatedKeyboard =  keyboardProvider.updateKeyboard(sourceKeyboard, newPage);
        String text = switch (pageSource) {
            case COLLECTIONS -> messageProvider.getText("collections"); // todo might be also extracted from source
            // page with create button
            default -> "UNKNOWN PAGE TYPE";
        };
        client.editCallbackMessage(text, updatedKeyboard);
    }
}
