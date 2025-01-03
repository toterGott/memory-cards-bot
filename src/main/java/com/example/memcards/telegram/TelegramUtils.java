package com.example.memcards.telegram;

import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@UtilityClass
public class TelegramUtils {

    public static ThreadLocal<TelegramUser> telegramUserThreadLocal = new ThreadLocal<>();
    public static ThreadLocal<Update> updateThreadLocal = new ThreadLocal<>();

    public static final String CALLBACK_DELIMITER = " ";

    public static TelegramUser getUser() {
        return telegramUserThreadLocal.get();
    }

    public static AvailableLocale getLanguage() {
        return telegramUserThreadLocal.get().getLanguage();
    }

    public static Locale getLocale() {
        return telegramUserThreadLocal.get().getLanguage().getLocale();
    }

    public static Long getChatId() {
        return telegramUserThreadLocal.get().getChatId();
    }

    public static Update getUpdate() {
        return updateThreadLocal.get();
    }

    public static Message getMessage() {
        return updateThreadLocal.get().getMessage();
    }

    public static CallbackQuery getCallback() {
        return updateThreadLocal.get().getCallbackQuery();
    }

    public static Integer getCallbackMessageId() {
        return updateThreadLocal.get().getCallbackQuery().getMessage().getMessageId();
    }
}
