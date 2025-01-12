package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getCallback;
import static com.totergott.memcards.telegram.TelegramUtils.getCallbackMessageId;
import static com.totergott.memcards.telegram.TelegramUtils.getChatId;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;

import com.totergott.memcards.user.TelegramUser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private static final int CHUNK_SIZE = 100;

    private final TelegramClient telegramClient;

    public Message sendMessage(TelegramUser user, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage(user.getChatId().toString(), text);

        sendMessage.setReplyMarkup(replyKeyboard);
        try {
            var message = telegramClient.execute(sendMessage);
            getUser().getPayload().getChatMessages().add(message.getMessageId());
            return message;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public Message sendMessage(String text) {
        return sendMessage(text, null);
    }

    public Message sendMessage(String text, ReplyKeyboard replyKeyboard) {
        return sendMessage(getUser(), text, replyKeyboard);
    }

    public void sendMessage(TelegramUser user, String text) {
        sendMessage(user, text, null);
    }

    public void answerCallback(AnswerCallbackQuery answer) {
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

    public void editCallbackMessage(String text) {
        editCallbackMessage(text, null);
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
        answerCallback(answer);
    }

    public void showAlertNotImplemented() {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(getCallback().getId());
        answer.setShowAlert(true);
        answer.setText("NOT IMPLEMENTED");
        answerCallback(answer);
    }

    public void clearCallbackKeyboard() {
        var sameText = ((Message) (getCallback().getMessage())).getText();
        editMessage(sameText, getCallbackMessageId(), null);
    }

    public void deleteMessagesExceptLast(Integer keepLast) {
        AtomicInteger counter = new AtomicInteger(0);
        var chatMessages = getUser().getPayload().getChatMessages();
        var chatMessagesToRemove = chatMessages.subList(0, chatMessages.size() - keepLast);
        chatMessagesToRemove
            .stream()
            .collect(Collectors.groupingBy(_ -> counter.getAndIncrement() / CHUNK_SIZE))
            .values()
            .forEach(messageIds -> {
                var request = new DeleteMessages(getChatId().toString(), messageIds);
                execute(request);
            });
        chatMessages.removeAll(chatMessagesToRemove);
    }

    public void editCallbackKeyboard(InlineKeyboardMarkup keyboard) {
        var sameText = ((Message) (getCallback().getMessage())).getText();
        editCallbackMessage(sameText, keyboard);
    }
}
