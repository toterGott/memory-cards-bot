package com.totergott.memcards.telegram.callback;

import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.user.TelegramUser;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {

    void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user);

    CallbackSource getCallbackSource();
}
