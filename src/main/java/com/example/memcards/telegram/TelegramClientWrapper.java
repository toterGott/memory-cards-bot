package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.getCallback;
import static com.example.memcards.telegram.TelegramUtils.getCallbackMessageId;
import static com.example.memcards.telegram.TelegramUtils.getChatId;

import com.example.memcards.user.TelegramUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramClientWrapper {

    private final TelegramClient telegramClient;

    public Message sendMessage(TelegramUser user, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(user.getChatId().toString(), text);

        sendMessage.setReplyMarkup(replyKeyboard);
        try {
            return telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public Message sendMessage(String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(getChatId().toString(), text);

        sendMessage.setReplyMarkup(replyKeyboard);
        try {
            return telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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

    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
        execute(deleteMessage);
    }

    public void deleteCallbackMessage() {
        DeleteMessage deleteMessage = new DeleteMessage(getChatId().toString(), getCallbackMessageId());
        execute(deleteMessage);
    }

    public void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup inlineKeyboard) {
        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(chatId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setReplyMarkup(inlineKeyboard);
        execute(editMessageText);
    }

    public void editCallbackMessage(String text, InlineKeyboardMarkup inlineKeyboard) {
        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(getChatId().toString());
        editMessageText.setMessageId(getCallbackMessageId());
        editMessageText.setReplyMarkup(inlineKeyboard);
        execute(editMessageText);
    }

    public void editMessage(String text, Integer messageId, InlineKeyboardMarkup inlineKeyboard) {
        EditMessageText editMessageText = new EditMessageText(text);
        editMessageText.setChatId(getChatId().toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setReplyMarkup(inlineKeyboard);
        execute(editMessageText);
    }

    public void showAlert(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        answer.setShowAlert(true);
        answer.setText(text);
        execute(answer);
    }

    public void showAlertNotImplemented() {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(getCallback().getId());
        answer.setShowAlert(true);
        answer.setText("NOT IMPLEMENTED");
        execute(answer);
    }
}
