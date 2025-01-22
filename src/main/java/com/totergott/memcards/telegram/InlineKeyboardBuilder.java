package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.totergott.memcards.telegram.callback.model.Callback;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

public class InlineKeyboardBuilder {
    private final List<InlineKeyboardRow> rows = new ArrayList<>();
    private InlineKeyboardRow currentRow = new InlineKeyboardRow();

    public InlineKeyboardBuilder () {
        rows.add(currentRow);
    }

    public InlineKeyboardBuilder addButton(String text, Callback callback) {
        var button = new InlineKeyboardButton(text);
        button.setCallbackData(writeCallback(callback));
        currentRow.add(button);
        return this;
    }

    public InlineKeyboardBuilder addRow() {
        currentRow = new InlineKeyboardRow();
        rows.add(currentRow);
        return this;
    }

    public InlineKeyboardMarkup build() {
        return new InlineKeyboardMarkup(rows);
    }
}
