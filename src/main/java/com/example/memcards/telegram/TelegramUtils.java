package com.example.memcards.telegram;

import com.example.memcards.user.TelegramUser;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;

@UtilityClass
public class TelegramUtils {

    public static ThreadLocal<TelegramUser> telegramUserThreadLocal = new ThreadLocal<>();
    public static ThreadLocal<Update> updateThreadLocal = new ThreadLocal<>();

    public static final String CALLBACK_DELIMITER = " ";
}
