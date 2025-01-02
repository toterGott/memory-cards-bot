package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;
import static com.example.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.example.memcards.collection.CardCollection;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.callback.CallbackAction;
import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CollectionsCallback;
import com.example.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    public InlineKeyboardMarkup buildCollectionsPage(AvailableLocale language, Page<CardCollection> page) {
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .build();

        List<InlineKeyboardRow> rows = new ArrayList<>();

        callback.setAction(CollectionCallbackAction.NEW_COLLECTION);
        var newCollections = new InlineKeyboardButton(messageProvider.getMessage("button.collection.new", language));
        newCollections.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(newCollections));

        callback.setAction(CollectionCallbackAction.SELECT);
        var pageContentRows = buildCollectionsPageContent(page, callback);
        rows.addAll(pageContentRows);

        callback.setAction(CollectionCallbackAction.CHANGE_PAGE);
        rows.add(buildPageNavigationRow(page, callback, language));

        return new InlineKeyboardMarkup(rows);
    }

    private List<InlineKeyboardRow> buildCollectionsPageContent(Page<CardCollection> page, Callback callback) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (CardCollection collection : page.getContent()) {
            callback.setData(collection.getId().toString());

            var button = new InlineKeyboardButton(collection.getName());
            button.setCallbackData(writeCallback(callback));
            rows.add(new InlineKeyboardRow(button));
        }
        return rows;
    }

    private InlineKeyboardRow buildPageNavigationRow(
        Page<CardCollection> page,
        Callback callback,
        AvailableLocale language
    ) {
        var pageNavigationRow = new InlineKeyboardRow();
        if (page.hasPrevious()) {
            callback.setData(String.valueOf(page.getNumber() - 1));
            var backButton = new InlineKeyboardButton(messageProvider.getMessage("button.previous_page", language));
            backButton.setCallbackData(writeCallback(callback));
            pageNavigationRow.add(backButton);
        }
        if (page.hasNext()) {
            callback.setData(String.valueOf(page.getNumber() + 1));
            var forwardButton = new InlineKeyboardButton(messageProvider.getMessage("button.next_page", language));
            forwardButton.setCallbackData(writeCallback(callback));
            pageNavigationRow.add(forwardButton);
        }
        return pageNavigationRow;
    }

    public InlineKeyboardMarkup buildCollectionPageForCardSelectionOnCreation(
        AvailableLocale language,
        Page<CardCollection> page
    ) {
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .action(CollectionCallbackAction.SELECT)
            .build();

        var pageContentRows = buildCollectionsPageContent(page, callback);
        List<InlineKeyboardRow> rows = new ArrayList<>(pageContentRows);

        callback.setAction(CollectionCallbackAction.CHANGE_PAGE);
        rows.add(buildPageNavigationRow(page, callback, language));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildCollectionSelectedOptionsKeyboard(AvailableLocale language, UUID collectionId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .action(CollectionCallbackAction.FOCUS_ON_COLLECTION)
            .data(collectionId.toString())
            .build();

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

        var changeCollectionButton = new InlineKeyboardButton(messageProvider.getMessage(
            "button.card.change_collection",
            user.getLanguage()
        ));
        changeCollectionButton.setCallbackData(CallbackAction.CHANGE_CARD_COLLECTION + CALLBACK_DELIMITER + cardId);
        rows.add(new InlineKeyboardRow(changeCollectionButton));

        var confirmText = messageProvider.getMessage("button.card.confirm_creation", user.getLanguage());
        var okButton = new InlineKeyboardButton(confirmText);
        okButton.setCallbackData(CallbackAction.CONFIRM_CARD_CREATION.name());
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
}
