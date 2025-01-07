package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.getLanguage;
import static com.example.memcards.telegram.TelegramUtils.getUser;
import static com.example.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.example.memcards.collection.CardCollection;
import com.example.memcards.common.PageableEntity;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CardCallback;
import com.example.memcards.telegram.callback.model.CardCallback.CardCallbackAction;
import com.example.memcards.telegram.callback.model.CollectionsCallback;
import com.example.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.example.memcards.telegram.callback.model.NewCardCallback;
import com.example.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.example.memcards.telegram.callback.model.ScheduleCallback;
import com.example.memcards.telegram.callback.model.ScheduleCallback.ScheduleCallbackAction;
import com.example.memcards.telegram.callback.model.SettingsCallback;
import com.example.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaContext;
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

    public ReplyKeyboardMarkup getMainMenu() {
        return getMainMenu(getUser());
    }

    public ReplyKeyboardMarkup getMainMenu(TelegramUser user) {
        var languageCode = user.getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.collections", languageCode));
        row.add(messageProvider.getMessage("button.schedule", languageCode));
        row.add(messageProvider.getMessage("button.settings", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        if (user.getFocusedOnCollection() != null) {
            row.add(messageProvider.getMessage("button.remove_focus", languageCode));
        }
        row.add(messageProvider.getMessage("button.create_card", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.get_card", languageCode));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getSettingsMenu(TelegramUser user) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        SettingsCallback callback = SettingsCallback.builder()
            .source(CallbackSource.SETTINGS)
            .action(SettingsCallbackAction.LANGUAGE)
            .build();

        var text = messageProvider.getMessage("button.settings.language", user.getLanguage());
        var languageChangeButton = new InlineKeyboardButton(text);
        languageChangeButton.setCallbackData(writeCallback(callback));
        keyboard.add(new InlineKeyboardRow(languageChangeButton));
        return new InlineKeyboardMarkup(keyboard);
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

    public ReplyKeyboardMarkup getShowAnswerKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getText("button.show_answer"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup buildCollectionsPage(Page<CardCollection> page) {
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .build();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        callback.setAction(CollectionCallbackAction.NEW_COLLECTION);
        var newCollections = new InlineKeyboardButton(messageProvider.getText("button.collection.new"));
        newCollections.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(newCollections));

        callback.setAction(CollectionCallbackAction.SELECT);
        var pageContentRows = buildCollectionsPageContent(page, callback);
        rows.addAll(pageContentRows);

        callback.setAction(CollectionCallbackAction.CHANGE_PAGE);
        rows.add(buildPageNavigationRow(page, callback));

        return new InlineKeyboardMarkup(rows);
    }

    private List<InlineKeyboardRow> buildCollectionsPageContent(
        Page<? extends PageableEntity> page,
        Callback callback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (PageableEntity collection : page.getContent()) {
            callback.setData(collection.getId().toString());

            var button = new InlineKeyboardButton(collection.getName());
            button.setCallbackData(writeCallback(callback));
            rows.add(new InlineKeyboardRow(button));
        }
        return rows;
    }

    private InlineKeyboardRow buildPageNavigationRow(
        Page<?> page,
        Callback callback
    ) {
        var pageNavigationRow = new InlineKeyboardRow();
        if (page.hasPrevious()) {
            callback.setData(String.valueOf(page.getNumber() - 1));
            var backButton = new InlineKeyboardButton(messageProvider.getText("button.previous_page"));
            backButton.setCallbackData(writeCallback(callback));
            pageNavigationRow.add(backButton);
        }
        if (page.hasNext()) {
            callback.setData(String.valueOf(page.getNumber() + 1));
            var forwardButton = new InlineKeyboardButton(messageProvider.getText("button.next_page"));
            forwardButton.setCallbackData(writeCallback(callback));
            pageNavigationRow.add(forwardButton);
        }
        return pageNavigationRow;
    }

    // todo shoulb page callback handler
    public InlineKeyboardMarkup buildCollectionPageForCardSelectionOnCreation(
        Page<CardCollection> page,
        Callback pageCallback,
        Callback navigationCallback
    ) {
        var pageContentRows = buildCollectionsPageContent(page, pageCallback);
        List<InlineKeyboardRow> rows = new ArrayList<>(pageContentRows);

        rows.add(buildPageNavigationRow(page, navigationCallback));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildCollectionSelectedOptionsKeyboard(AvailableLocale language, UUID collectionId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .data(collectionId.toString())
            .build();

        callback.setAction(CollectionCallbackAction.FOCUS_ON_COLLECTION);
        var choose = new InlineKeyboardButton(messageProvider.getMessage("button.collection.choose", language));
        choose.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(choose));

        callback.setAction(CollectionCallbackAction.EDIT_CARDS);
        var editCards = new InlineKeyboardButton(messageProvider.getMessage("button.collection.edit", language));
        editCards.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(editCards));

        callback.setAction(CollectionCallbackAction.DELETE);
        var delete = new InlineKeyboardButton(messageProvider.getMessage("button.collection.delete", language));
        delete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(delete));

        callback.setAction(CollectionCallbackAction.BACK);
        var back = new InlineKeyboardButton(messageProvider.getMessage("button.collection.back", language));
        back.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(back));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getCardCreatedInlineKeyboard(TelegramUser user, UUID cardId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        NewCardCallback callback = NewCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .build();

        callback.setAction(NewCardCallbackAction.CHANGE_COLLECTION);
        callback.setData(cardId.toString());
        var changeCollectionButton = new InlineKeyboardButton(messageProvider.getMessage(
            "button.card.change_collection",
            user.getLanguage()
        ));
        changeCollectionButton.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(changeCollectionButton));

        callback.setAction(NewCardCallbackAction.CONFIRM);
        var confirmText = messageProvider.getMessage("button.card.confirm_creation", user.getLanguage());
        var okButton = new InlineKeyboardButton(confirmText);
        okButton.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(okButton));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildDeleteConfirmationDialog(TelegramUser user, UUID collectionId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .action(CollectionCallbackAction.CONFIRM_DELETE)
            .data(collectionId.toString())
            .build();

        var confirmDelete = new InlineKeyboardButton(messageProvider.getMessage(
            "button.collection.delete.confirm",
            user.getLanguage()
        ));
        confirmDelete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(confirmDelete));

        callback.setAction(CollectionCallbackAction.SELECT);
        var back = new InlineKeyboardButton(messageProvider.getMessage(
            "button.collection.delete.back",
            user.getLanguage()
        ));
        back.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(back));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildLanguageKeyboard() {
        var row = new InlineKeyboardRow();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        SettingsCallback callback = SettingsCallback.builder()
            .source(CallbackSource.SETTINGS)
            .action(SettingsCallbackAction.CHANNEL_LANGUAGE)
            .build();

        callback.setData(AvailableLocale.EN.name());
        var eng = new InlineKeyboardButton(AvailableLocale.EN.getReadableName());
        eng.setCallbackData(writeCallback(callback));
        row.add(eng);

        callback.setData(AvailableLocale.RU.name());
        var rus = new InlineKeyboardButton(AvailableLocale.RU.getReadableName());
        rus.setCallbackData(writeCallback(callback));
        row.add(rus);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getCardDeleteConfirmation(String cardId) {
        var row = new InlineKeyboardRow();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        var callback = CardCallback.builder()
            .source(CallbackSource.CARD)
            .action(CardCallbackAction.DELETE_CONFIRM)
            .data(cardId)
            .build();

        var text = messageProvider.getText("button.card.delete");
        var delete = new InlineKeyboardButton(text);
        delete.setCallbackData(writeCallback(callback));
        row.add(delete);

        callback.setAction(CardCallbackAction.CANCEL);
        text = messageProvider.getText("button.card.cancel");
        var cancel = new InlineKeyboardButton(text);
        cancel.setCallbackData(writeCallback(callback));
        row.add(cancel);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getAfterCardAnswer(UUID cardId) {
        var row = new InlineKeyboardRow();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        var callback = CardCallback.builder()
            .source(CallbackSource.CARD)
            .action(CardCallbackAction.DELETE)
            .data(cardId.toString())
            .build();

        var text = messageProvider.getText("button.card.delete");
        var delete = new InlineKeyboardButton(text);
        delete.setCallbackData(writeCallback(callback));
        row.add(delete);

        callback.setAction(CardCallbackAction.CHANGE_COLLECTION);
        text = messageProvider.getText("button.card.change_collection");
        var changeCollection = new InlineKeyboardButton(text);
        changeCollection.setCallbackData(writeCallback(callback));
        row.add(changeCollection);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getScheduleKeyboard() {
        var row = new InlineKeyboardRow();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        var callback = ScheduleCallback.builder()
            .source(CallbackSource.SCHEDULE)
            .action(ScheduleCallbackAction.SET_TIME)
            .build();

        callback.setData("1");
        var text = messageProvider.getText("button.schedule.settings.time", callback.getData());
        var timeButton = new InlineKeyboardButton(text);
        timeButton.setCallbackData(writeCallback(callback));
        row.add(timeButton);

        callback.setData("3");
        text = messageProvider.getText("button.schedule.settings.time", callback.getData());
        timeButton = new InlineKeyboardButton(text);
        timeButton.setCallbackData(writeCallback(callback));
        row.add(timeButton);

        row = new InlineKeyboardRow();
        keyboard.add(row);

        callback.setData("6");
        text = messageProvider.getText("button.schedule.settings.time", callback.getData());
        timeButton = new InlineKeyboardButton(text);
        timeButton.setCallbackData(writeCallback(callback));
        row.add(timeButton);

        callback.setData(null);
        callback.setAction(ScheduleCallbackAction.DISABLE);
        text = messageProvider.getText("button.schedule.settings.disable", callback.getData());
        timeButton = new InlineKeyboardButton(text);
        timeButton.setCallbackData(writeCallback(callback));
        row.add(timeButton);

        return new InlineKeyboardMarkup(keyboard);
    }
}
