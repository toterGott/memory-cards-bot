package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.CallbackMapper.readCallback;
import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.totergott.memcards.common.PageableEntity;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CardCallback;
import com.totergott.memcards.telegram.callback.model.CardCallback.CardCallbackAction;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback.PageNavigationCallbackAction;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback.ScheduleCallbackAction;
import com.totergott.memcards.telegram.callback.model.SettingsCallback;
import com.totergott.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;
import com.totergott.memcards.user.AvailableLocale;
import com.totergott.memcards.user.TelegramUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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
        row.add(messageProvider.getMessage("button.get_card", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        if (user.getFocusedOnCollection() != null) {
            row.add(messageProvider.getMessage("button.remove_focus", languageCode));
        }
        row.add(messageProvider.getMessage("button.new_card", languageCode));
        row.add(messageProvider.getMessage("button.new_collection", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(messageProvider.getMessage("button.collections", languageCode));
        row.add(messageProvider.getMessage("button.schedule", languageCode));
        row.add(messageProvider.getMessage("button.settings", languageCode));
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

        var text = messageProvider.getText("emoji.language")
            + messageProvider.getText("button.settings.language");
        var languageChangeButton = new InlineKeyboardButton(text);
        languageChangeButton.setCallbackData(writeCallback(callback));
        keyboard.add(new InlineKeyboardRow(languageChangeButton));
        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getInlineKnowledgeCheckKeyboard(UUID cardId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        rows.add(row);
        CardCallback callback =
            CardCallback.builder().action(CardCallbackAction.CHECK_KNOWLEDGE).data(cardId.toString()).build();

        InlineKeyboardButton againButton = new InlineKeyboardButton(messageProvider.getText("button.again"));
        callback.setAdditionalData("0");
        againButton.setCallbackData(writeCallback(callback));
        row.add(againButton);

        InlineKeyboardButton hardButton = new InlineKeyboardButton(messageProvider.getText("button.hard"));
        callback.setAdditionalData("1");
        hardButton.setCallbackData(writeCallback(callback));
        row.add(hardButton);

        row = new InlineKeyboardRow();
        rows.add(row);

        InlineKeyboardButton goodButton = new InlineKeyboardButton(messageProvider.getText("button.good"));
        callback.setAdditionalData("2");
        goodButton.setCallbackData(writeCallback(callback));
        row.add(goodButton);

        InlineKeyboardButton easyButton = new InlineKeyboardButton(messageProvider.getText("button.easy"));
        callback.setAdditionalData("3");
        easyButton.setCallbackData(writeCallback(callback));
        row.add(easyButton);

        return new InlineKeyboardMarkup(rows);
    }

    public ReplyKeyboardMarkup getShowAnswerKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getText("emoji.answer"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getCardPlaceholder() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getText("emoji.card"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getInlineShowAnswerKeyboard(UUID id) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        rows.add(row);

        CardCallback callback =
            CardCallback.builder().action(CardCallbackAction.SHOW_ANSWER).data(id.toString()).build();
        InlineKeyboardButton showAnswerButton = new InlineKeyboardButton(
            messageProvider.getText("emoji.answer")
                + messageProvider.getText("card.show_answer")
        );
        showAnswerButton.setCallbackData(writeCallback(callback));
        row.add(showAnswerButton);

        return new InlineKeyboardMarkup(rows);
    }

    private List<InlineKeyboardRow> buildCollectionsPageContent(
        Page<? extends PageableEntity> page,
        Callback callback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (PageableEntity collection : page.getContent()) {
            callback.setData(collection.getId().toString());
            callback.setAdditionalData(String.valueOf(page.getNumber()));

            var button = new InlineKeyboardButton(collection.getName());
            button.setCallbackData(writeCallback(callback));
            rows.add(new InlineKeyboardRow(button));
        }
        return rows;
    }

    public InlineKeyboardMarkup buildCollectionSelectedOptionsKeyboard(
        AvailableLocale language,
        UUID collectionId,
        String pageNumber
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .data(collectionId.toString())
            .build();

        callback.setAction(CollectionCallbackAction.FOCUS_ON_COLLECTION);
        var choose = new InlineKeyboardButton(messageProvider.getMessage("collection.button.choose", language));
        choose.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(choose));

        callback.setAction(CollectionCallbackAction.EDIT_CARDS);
        var editCards = new InlineKeyboardButton(messageProvider.getMessage("collection.button.edit", language));
        editCards.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(editCards));

        callback.setAction(CollectionCallbackAction.BACK);
        callback.setAdditionalData(pageNumber);
        var back = new InlineKeyboardButton(messageProvider.getMessage("collection.button.back", language));
        back.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(back));

        callback.setAction(CollectionCallbackAction.DELETE);
        var delete = new InlineKeyboardButton(
            messageProvider.getText("emoji.delete")
                + messageProvider.getText("button.delete")
        );
        delete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(delete));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getCardCreatedInlineKeyboard(UUID cardId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        var row = new InlineKeyboardRow();
        rows.add(row);

        NewCardCallback callback = NewCardCallback.builder()
            .source(CallbackSource.NEW_CARD)
            .build();
        callback.setData(cardId.toString());

        var cardCallback = new CardCallback();
        cardCallback.setData(cardId.toString());

        cardCallback.setAction(CardCallbackAction.DELETE);
        var text = messageProvider.getText("button.card.delete");
        var delete = new InlineKeyboardButton(text);
        delete.setCallbackData(writeCallback(cardCallback));
        row.add(delete);

        callback.setAction(NewCardCallbackAction.CHANGE_COLLECTION);
        var changeCollectionButton = new InlineKeyboardButton(messageProvider.getText(
            "button.card.change_collection"
        ));
        changeCollectionButton.setCallbackData(writeCallback(callback));
        row.add(changeCollectionButton);

        rows.add(row = new InlineKeyboardRow());
        callback.setAction(NewCardCallbackAction.CONFIRM);
        var confirmText = messageProvider.getText("button.card.confirm_creation");
        var okButton = new InlineKeyboardButton(confirmText);
        okButton.setCallbackData(writeCallback(callback));
        row.add(okButton);

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildDeleteConfirmationDialog(TelegramUser user, UUID collectionId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        CollectionsCallback callback = CollectionsCallback.builder()
            .source(CallbackSource.COLLECTIONS)
            .action(CollectionCallbackAction.CONFIRM_DELETE)
            .data(collectionId.toString())
            .build();

        var confirmDelete = new InlineKeyboardButton(
            messageProvider.getText("emoji.delete")
                + messageProvider.getText("button.delete"));
        confirmDelete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(confirmDelete));

        callback.setAction(CollectionCallbackAction.SELECT);
        var back = new InlineKeyboardButton(messageProvider.getMessage(
            "button.back",
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

    public InlineKeyboardMarkup getCardMenuAfterAnswerWithOptions(UUID cardId) {
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

        row = new InlineKeyboardRow();
        callback.setAction(CardCallbackAction.CHANGE_COLLECTION);
        text = messageProvider.getText("button.card.change_collection");
        var changeCollection = new InlineKeyboardButton(text);
        changeCollection.setCallbackData(writeCallback(callback));
        row.add(changeCollection);
        keyboard.add(row);

        row = new InlineKeyboardRow();
        callback.setAction(CardCallbackAction.EDIT);
        text = messageProvider.getText("button.card.edit");
        var editCard = new InlineKeyboardButton(text);
        editCard.setCallbackData(writeCallback(callback));
        row.add(editCard);
        keyboard.add(row);

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getCardMenuAfterAnswer(UUID cardId) {
        var row = new InlineKeyboardRow();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        var callback = CardCallback.builder()
            .source(CallbackSource.CARD)
            .action(CardCallbackAction.CONFIGS)
            .data(cardId.toString())
            .build();

        var text = messageProvider.getText("button.card.configure");
        var configureButton = new InlineKeyboardButton(text);
        configureButton.setCallbackData(writeCallback(callback));
        row.add(configureButton);

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

    public InlineKeyboardMarkup buildPage(
        Page<? extends PageableEntity> page,
        Callback pageItemsCallback
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>(buildCollectionsPageContent(page, pageItemsCallback));

        rows.add(buildDefaultNavigationRow(page));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardRow buildDefaultNavigationRow(Page<? extends PageableEntity> page) {
        PageNavigationCallback navigationCallback = new PageNavigationCallback();
        var pageNavigationRow = new InlineKeyboardRow();

        if (page.hasPrevious()) {
            var index = String.valueOf(page.getNumber() - 1);
            var prevPageNum = String.valueOf(page.getNumber() + 1 - 1);
            navigationCallback.setAction(PageNavigationCallbackAction.PREVIOUS);
            navigationCallback.setData(index);
            var backButton = new InlineKeyboardButton(messageProvider.getText("button.previous_page", prevPageNum));
            backButton.setCallbackData(writeCallback(navigationCallback));
            pageNavigationRow.add(backButton);
        }

        if (page.hasNext()) {
            var index = String.valueOf(page.getNumber() + 1);
            var nextPageNum = String.valueOf(page.getNumber() + 1 + 1);
            navigationCallback.setAction(PageNavigationCallbackAction.NEXT);
            navigationCallback.setData(index);
            var forwardButton = new InlineKeyboardButton(messageProvider.getText(
                "button.next_page",
                nextPageNum
            ));
            forwardButton.setCallbackData(writeCallback(navigationCallback));
            pageNavigationRow.add(forwardButton);
        }

        return pageNavigationRow;
    }

    public InlineKeyboardMarkup updateKeyboard(
        InlineKeyboardMarkup sourceKeyboard,
        Page<? extends PageableEntity> newPage
    ) {
        var callback = readCallback(sourceKeyboard.getKeyboard().getFirst().getFirst().getCallbackData());
        List<InlineKeyboardRow> rows = new ArrayList<>(buildCollectionsPageContent(newPage, callback));

        rows.add(buildDefaultNavigationRow(newPage));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup buildCardKeyboard(UUID cardId, String pageNumber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        var row = new InlineKeyboardRow();
        rows.add(row);
        var callback = new CardCallback();
        callback.setAdditionalData(pageNumber);
        callback.setData(cardId.toString());

        callback.setAction(CardCallbackAction.DELETE);
        var deleteButton = new InlineKeyboardButton(messageProvider.getText("button.card.delete"));
        deleteButton.setCallbackData(writeCallback(callback));
        row.add(deleteButton);

        callback.setAction(CardCallbackAction.BACK);
        var backButton = new InlineKeyboardButton(messageProvider.getText("button.back"));
        backButton.setCallbackData(writeCallback(callback));
        row.add(backButton);

        return new InlineKeyboardMarkup(rows);
    }

    public ReplyKeyboardMarkup getCollectionsPlaceholder() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(messageProvider.getText("emoji.collection"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getOneSingleButton(String text) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(text);
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getOneInlineButton(String text, Callback callback) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        var row = new InlineKeyboardRow();
        rows.add(row);

        var button = new InlineKeyboardButton(text);
        button.setCallbackData(writeCallback(callback));
        row.add(button);

        return new InlineKeyboardMarkup(rows);
    }

    public ReplyKeyboard getBackToMainMenuReply() {
        return getOneSingleButton(messageProvider.getText("button.back_to_main_menu"));
    }
}
