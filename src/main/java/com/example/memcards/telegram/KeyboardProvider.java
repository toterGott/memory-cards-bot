package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;
import static com.example.memcards.telegram.TelegramUtils.telegramUserThreadLocal;

import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.AvailableLocale;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class KeyboardProvider {

    private final MessageProvider messageProvider;

    public ReplyKeyboardMarkup getMainMenu(AvailableLocale languageCode) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.collections", languageCode));
        row.add(messageProvider.getMessage("button.info", languageCode));
        row.add(messageProvider.getMessage("button.settings", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.get_card", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.create_card", languageCode));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getSettingsMenu() {
        InlineKeyboardRow row = new InlineKeyboardRow();
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(List.of(row));

        var eng = new InlineKeyboardButton(AvailableLocale.EN.getName());
        eng.setCallbackData(CallbackAction.SET_LANGUAGE + CALLBACK_DELIMITER + AvailableLocale.EN);
        row.add(eng);

        var rus = new InlineKeyboardButton(AvailableLocale.RU.getName());
        rus.setCallbackData(CallbackAction.SET_LANGUAGE + CALLBACK_DELIMITER + AvailableLocale.RU);
        row.add(rus);

        return keyboardMarkup;
    }

    public ReplyKeyboardRemove hideKeyboard() {
        return new ReplyKeyboardRemove(true);
    }

    public ReplyKeyboardMarkup getKnowledgeCheckKeyboard(AvailableLocale languageCode) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.again", languageCode));
        row.add(messageProvider.getMessage("button.hard", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.good", languageCode));
        row.add(messageProvider.getMessage("button.easy", languageCode));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getShowAnswerKeyboard(AvailableLocale languageCode) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.show_answer", languageCode));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }
}
