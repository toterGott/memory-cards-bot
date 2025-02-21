package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getChatId;
import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.COLLECTION_CREATION;

import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.callback.handler.CreateEditCardScreenHandler;
import com.totergott.memcards.telegram.callback.handler.GetCardHandler;
import com.totergott.memcards.telegram.callback.handler.ScheduleCallbackHandler;
import com.totergott.memcards.user.TelegramUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplyKeyboardButtonHandler {

    @Value("${app.version}")
    private String version;

    private final KeyboardProvider keyboardProvider;
    private final TextProvider textProvider;
    private final MessageService messageService;
    private final CreateEditCardScreenHandler createEditCardScreenHandler;
    private final CommonHandler commonHandler;
    private final GetCardHandler getCardHandler;
    private final ScheduleCallbackHandler scheduleHandler;

    public void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = textProvider.resolveCode(text);
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.get_card" -> getCardHandler.showCard();
            case "button.new_card" -> createEditCardScreenHandler.startCreateCardDialog();
            case "button.new_collection" -> createCollection();
            case "button.schedule" -> scheduleHandler.handleSchedule();
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> commonHandler.collectionsScreen();
            case "button.remove_focus" -> removeFocus(user);
            case "button.back_to_main_menu" -> commonHandler.setMainMenu();
            default -> handleUnknownMessage();
        }
    }

    private void removeFocus(TelegramUser user) {
        user.setFocusedOnCollection(null);
        var keyboard = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(
            textProvider.getMessage("collections.focus_removed", user.getLanguage()),
            keyboard
        );
    }

    private void sendSettingsMessage(TelegramUser user) {
        messageService.sendMessage(
            textProvider.get("emoji.settings"),
            keyboardProvider.getBackToMainMenuReply()
        );
        var text = "v." + version + "\n" + textProvider.get("settings");
        var settingsKeyboard = keyboardProvider.getSettingsMenu(user);
        messageService.sendMessage(text, settingsKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    private void handleUnknownMessage() {
        messageService.deleteMessage(getChatId(), getMessage().getMessageId());
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        messageService.sendMessage(textProvider.get("emoji.create"));
        var text = textProvider.get("create.collection.name_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
        messageService.deleteMessagesExceptLast(2);
    }
}
