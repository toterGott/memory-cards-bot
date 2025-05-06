package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.Constants.MENU_COMMAND;
import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static com.totergott.memcards.telegram.Constants.STATS_COMMAND;
import static com.totergott.memcards.telegram.TelegramUtils.getUpdate;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.STAND_BY;

import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.user.TelegramUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandHandler {

    private final CommonHandler commonHandler;
    private final TextProvider textProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService messageService;

    public void handleCommand(MessageEntity messageEntity) {
        var user = getUser();
        switch (messageEntity.getText()) {
            case START_COMMAND -> {
                user.setState(STAND_BY);
                sendWelcomeMessage(user);
                messageService.deleteMessagesExceptLast(1);
            }
            case MENU_COMMAND -> commonHandler.setMainMenu();
            case STATS_COMMAND -> commonHandler.showStats();
            default -> log.warn("Unhandled command: {}", messageEntity.getText());
        }
    }

    private void sendWelcomeMessage(TelegramUser user) {
        var welcomeText = textProvider.getMessage("welcome", user.getLanguage());
        var mainMenu = keyboardProvider.getMainMenu(user);
        messageService.sendMessage(getUpdate().getMessage().getChatId(), welcomeText, mainMenu);
    }
}
