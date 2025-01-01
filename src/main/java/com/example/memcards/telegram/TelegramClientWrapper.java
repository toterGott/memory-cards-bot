package com.example.memcards.telegram;

import com.example.memcards.user.TelegramUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodSerializable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramClientWrapper {

    private final TelegramClient telegramClient;

    public void sendMessage(TelegramUser user, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(user.getChatId().toString(), text);

        sendMessage.setReplyMarkup(replyKeyboard);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error while sending a message", e);
        }
    }

    public void sendMessage(TelegramUser user, String text) {
        sendMessage(user, text, null);
    }

    public void execute(AnswerCallbackQuery answer) {
        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    public void execute(BotApiMethod<?> message) {
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
