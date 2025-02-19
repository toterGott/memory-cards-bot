package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.callback.CallbackMapper.readCallback;
import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.totergott.memcards.common.PageableEntity;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.GetCardCallback;
import com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback.PageNavigationCallbackAction;
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

    private final TextProvider textProvider;

    public ReplyKeyboardMarkup getMainMenu() {
        return getMainMenu(getUser());
    }

    public ReplyKeyboardMarkup getMainMenu(TelegramUser user) {
        var languageCode = user.getLanguage();
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(textProvider.getMessage("button.get_card", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        if (user.getFocusedOnCollection() != null) {
            row.add(textProvider.getMessage("button.remove_focus", languageCode));
        }
        row.add(textProvider.getMessage("button.new_card", languageCode));
        row.add(textProvider.getMessage("button.new_collection", languageCode));
        keyboard.add(row);

        row = new KeyboardRow();
        row.add(textProvider.getMessage("button.collections", languageCode));
        row.add(textProvider.getMessage("button.schedule", languageCode));
        row.add(textProvider.getMessage("button.settings", languageCode));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getSettingsMenu(TelegramUser user) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        SettingsCallback callback = SettingsCallback.builder()
            .action(SettingsCallbackAction.INFO)
            .build();

        var aboutText = textProvider.get("emoji.info")
            + textProvider.get("button.settings.info");
        var aboutButton = new InlineKeyboardButton(aboutText);
        aboutButton.setCallbackData(writeCallback(callback));
        keyboard.add(new InlineKeyboardRow(aboutButton));

        callback.setAction(SettingsCallbackAction.LANGUAGE);
        var text = textProvider.get("emoji.language")
            + textProvider.get("button.settings.language");
        var languageChangeButton = new InlineKeyboardButton(text);
        languageChangeButton.setCallbackData(writeCallback(callback));
        keyboard.add(new InlineKeyboardRow(languageChangeButton));

        return new InlineKeyboardMarkup(keyboard);
    }

    public InlineKeyboardMarkup getInlineKnowledgeCheckKeyboard(UUID cardId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        rows.add(row);
        GetCardCallback callback =
            GetCardCallback.builder().action(GetCardCallbackAction.CHECK_INFO).data(cardId.toString()).build();

        InlineKeyboardButton checkInfo = new InlineKeyboardButton(textProvider.get(
            "button.knowledge_check_info"));
        checkInfo.setCallbackData(writeCallback(callback));
        row.add(checkInfo);

        row = new InlineKeyboardRow();
        rows.add(row);

        callback.setAction(GetCardCallbackAction.CHECK_KNOWLEDGE);
        InlineKeyboardButton againButton = new InlineKeyboardButton(textProvider.get("button.again"));
        callback.setAdditionalData("0");
        againButton.setCallbackData(writeCallback(callback));
        row.add(againButton);

        InlineKeyboardButton hardButton = new InlineKeyboardButton(textProvider.get("button.hard"));
        callback.setAdditionalData("1");
        hardButton.setCallbackData(writeCallback(callback));
        row.add(hardButton);

        row = new InlineKeyboardRow();
        rows.add(row);

        InlineKeyboardButton goodButton = new InlineKeyboardButton(textProvider.get("button.good"));
        callback.setAdditionalData("2");
        goodButton.setCallbackData(writeCallback(callback));
        row.add(goodButton);

        InlineKeyboardButton easyButton = new InlineKeyboardButton(textProvider.get("button.easy"));
        callback.setAdditionalData("3");
        easyButton.setCallbackData(writeCallback(callback));
        row.add(easyButton);

        return new InlineKeyboardMarkup(rows);
    }

    public ReplyKeyboardMarkup getShowAnswerKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);

        KeyboardRow row = new KeyboardRow();
        row.add(textProvider.get("emoji.answer"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getInlineShowAnswerKeyboard(UUID id) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        rows.add(row);

        GetCardCallback callback =
            GetCardCallback.builder().action(GetCardCallbackAction.SHOW_ANSWER).data(id.toString()).build();
        InlineKeyboardButton showAnswerButton = new InlineKeyboardButton(
            textProvider.get("emoji.answer")
                + textProvider.get("card.show_answer")
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
        var choose = new InlineKeyboardButton(textProvider.getMessage("collection.button.choose", language));
        choose.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(choose));

        callback.setAction(CollectionCallbackAction.BROWSE_CARDS);
        var editCards = new InlineKeyboardButton(
            textProvider.get("emoji.card")
                + textProvider.get("collection.button.browse_cards")
        );
        editCards.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(editCards));

        callback.setAction(CollectionCallbackAction.DELETE);
        var delete = new InlineKeyboardButton(
            textProvider.get("emoji.delete")
                + textProvider.get("button.delete")
        );
        delete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(delete));

        callback.setAction(CollectionCallbackAction.BACK);
        callback.setAdditionalData(pageNumber);
        var back = new InlineKeyboardButton(
            textProvider.get("emoji.back")
                + textProvider.get("button.back")
        );
        back.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(back));

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
            textProvider.get("emoji.delete")
                + textProvider.get("button.delete"));
        confirmDelete.setCallbackData(writeCallback(callback));
        rows.add(new InlineKeyboardRow(confirmDelete));

        callback.setAction(CollectionCallbackAction.SELECT);
        var back = new InlineKeyboardButton(
            textProvider.get("emoji.back")
                + textProvider.get("button.back")
        );
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
            .action(SettingsCallbackAction.CHANGE_LANGUAGE)
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
            var backButton = new InlineKeyboardButton(textProvider.get("button.previous_page", prevPageNum));
            backButton.setCallbackData(writeCallback(navigationCallback));
            pageNavigationRow.add(backButton);
        }

        if (page.hasNext()) {
            var index = String.valueOf(page.getNumber() + 1);
            var nextPageNum = String.valueOf(page.getNumber() + 1 + 1);
            navigationCallback.setAction(PageNavigationCallbackAction.NEXT);
            navigationCallback.setData(index);
            var forwardButton = new InlineKeyboardButton(textProvider.get(
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
        return getOneSingleButton(textProvider.get("button.back_to_main_menu"));
    }
}
