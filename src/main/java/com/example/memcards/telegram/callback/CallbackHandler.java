package com.example.memcards.telegram.callback;

import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.user.TelegramUser;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {

    void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user);

    CallbackSource getCallbackSource();
}
