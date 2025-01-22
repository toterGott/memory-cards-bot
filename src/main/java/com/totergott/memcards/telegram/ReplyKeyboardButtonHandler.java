package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getChatId;
import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.COLLECTION_CREATION;
import static com.totergott.memcards.user.UserState.STAND_BY;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.handler.CreateEditCardScreenHandler;
import com.totergott.memcards.telegram.callback.handler.GetCardScreenHandler;
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
    private final MessageProvider messageProvider;
    private final MessageService messageService;
    private final CardService cardService;
    private final CreateEditCardScreenHandler createEditCardScreenHandler;
    private final CommonHandler commonHandler;
    private final GetCardScreenHandler getCardScreenHandler;

    public void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = messageProvider.resolveCode(text);
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.get_card" -> getCardScreenHandler.showCard();
            case "button.new_card" -> createEditCardScreenHandler.startCreateCardDialog();
            case "button.new_collection" -> createCollection();
            case "button.schedule" -> handleSchedule();
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> commonHandler.collectionsScreen();
            case "button.remove_focus" -> removeFocus(user);
            case "button.back_to_main_menu" -> mainMenu();
            default -> handleUnknownMessage();
        }
    }

    private void mainMenu() {
        messageService.checkoutMainMenu();
        getUser().setState(STAND_BY);
        var cardId = getUser().getCurrentCardId();
        if (cardId != null) {
            cardService.findById(cardId).ifPresentOrElse(
                card -> {
                    if (card.getQuestion() == null) {
                        cardService.deleteById(cardId);
                    }
                },
                () -> log.warn("User {} had currentCardId {} but card doesn't exist.", getUser().getId(), cardId)
            );
            getUser().setCurrentCardId(null);
        }
    }

    private void handleSchedule() {
        if (true) { // todo fix card sending by the scheduler
            messageService.sendMessage(
                messageProvider.getText("not_implemented"),
                keyboardProvider.getBackToMainMenuReply());
            messageService.deleteMessagesExceptLast(1);
            return;
        }
        var schedule = getUser().getPayload().getSchedule();
        String text;
        if (schedule != null) {
            text = messageProvider.getText("schedule.enabled", schedule.getHours().toString());
        } else {
            text = messageProvider.getText("schedule");
        }
        var keyboard = keyboardProvider.getScheduleKeyboard();
        messageService.sendMessage(
            messageProvider.getText("emoji.schedule"),
            keyboardProvider.getBackToMainMenuReply()
        );
        messageService.sendMessage(text, keyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    private void removeFocus(TelegramUser user) {
        user.setFocusedOnCollection(null);
        var keyboard = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(
            messageProvider.getMessage("collections.focus_removed", user.getLanguage()),
            keyboard
        );
    }

    private void sendSettingsMessage(TelegramUser user) {
        messageService.sendMessage(
            messageProvider.getText("emoji.settings"),
            keyboardProvider.getBackToMainMenuReply()
        );
        var text = "v." + version + "\n" + messageProvider.getText("settings");
        var settingsKeyboard = keyboardProvider.getSettingsMenu(user);
        messageService.sendMessage(text, settingsKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    private void handleUnknownMessage() {
        messageService.deleteMessage(getChatId(), getMessage().getMessageId());
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        messageService.sendMessage(messageProvider.getText("emoji.create"));
        var text = messageProvider.getText("create.collection.name_prompt");
        messageService.sendMessage(
            text,
            ForceReplyKeyboard.builder().forceReply(true).inputFieldPlaceholder(text).build()
        );
        messageService.deleteMessagesExceptLast(2);
    }
}
