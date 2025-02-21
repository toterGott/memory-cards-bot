package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUpdate;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.telegram.TelegramUtils.telegramUserThreadLocal;
import static com.totergott.memcards.telegram.TelegramUtils.updateThreadLocal;

import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.callback.TelegramCallbackDelegate;
import com.totergott.memcards.telegram.callback.handler.CreateEditCardScreenHandler;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateHandler {

    private final MessageService messageService;
    private final UserService userService;
    private final KeyboardProvider keyboardProvider;
    private final TextProvider textProvider;
    private final CollectionService collectionService;
    private final TelegramCallbackDelegate callbackHandler;
    private final ReplyKeyboardButtonHandler buttonHandler;
    private final CreateEditCardScreenHandler createEditCardScreenHandler;

    public static final String COMMAND_TYPE = "bot_command";
    private final CommandHandler commandHandler;
    private final CommonHandler commonHandler;

    @Transactional
    public void handleUpdate(Update update) {
        if (update.hasMessage()) {
            log.debug("Handling message {}", update.getMessage().getText());
        }
        if (update.hasCallbackQuery()) {
            log.debug("Handling callback {}", update.getCallbackQuery().getData());
        }

        try {
            updateThreadLocal.set(update);
            var user = welcomeOrGetUser(update);
            user.getPayload().setLastInteractionTimestamp(Instant.now());
            telegramUserThreadLocal.set(user);
            log.debug("User {} state before {}", user.getUsername(), user.getState());

            if (update.hasCallbackQuery()) {
                callbackHandler.handleCallback(update.getCallbackQuery(), user);
            } else if (update.hasMessage()) {
                user.getPayload().getChatMessages().add(update.getMessage().getMessageId());
                handleUpdateByUserState(update, user);
            }
            log.debug("User {} state after {}", user.getUsername(), user.getState());
        } catch (Exception e) {
            if (update.hasMessage()) {
                messageService.deleteMessage(getUpdate().getMessage().getChatId(), getMessage().getMessageId());
            }
            log.error("Error in update handler", e);
        } finally {
            telegramUserThreadLocal.remove();
            updateThreadLocal.remove();
        }
    }

    private void handleUpdateByUserState(Update update, TelegramUser user) {
        if (update.getMessage().getEntities() != null) {
            var command = update.getMessage().getEntities().stream()
                .filter(messageEntity -> messageEntity.getType().equals(COMMAND_TYPE))
                .findFirst();
            if (command.isPresent()) {
                commandHandler.handleCommand(command.get());
                return;
            }
        }
        switch (user.getState()) {
            case STAND_BY, QUESTION_SHOWED, EVALUATE_ANSWER -> handleStandBy(update, user);
            case WAIT_CARD_QUESTION_INPUT -> createEditCardScreenHandler.handleQuestionInput();
            case WAIT_CARD_ANSWER_INPUT -> createEditCardScreenHandler.handleAnswerInput();
            case COLLECTION_CREATION -> createCollection();
            default -> log.warn("Unhandled state: {} and input: {}", user.getState(), getMessage().getText());
        }
    }


    private void createCollection() {
        var text = collectionService.createCollection(getMessage().getText(), getUser()).map(
            collection -> textProvider.get("create.collection.created", collection.getName()))
            .orElse(textProvider.get("collection.limit_reached"));

        commonHandler.setMainMenu();
        messageService.sendMessage(text);
    }

    private void handleStandBy(Update update, TelegramUser user) {
        buttonHandler.handleButton(update, user);
    }


    private TelegramUser welcomeOrGetUser(Update update) {
        Chat chat;
        String languageCode;
        if (update.hasMessage()) {
            chat = update.getMessage().getChat();
            languageCode = update.getMessage().getFrom().getLanguageCode();
        } else {
            chat = update.getCallbackQuery().getMessage().getChat();
            languageCode = update.getCallbackQuery().getFrom().getLanguageCode();
        }

        var user = userService.getUserByTelegramId(chat.getId());
        if (user.isPresent()) {
            return userService.updateUserInfoIfNeeded(user.get(), chat);
        }

        var newUser = userService.createUser(chat, languageCode);

        collectionService.initDefaultCollection(newUser);
        collectionService.initTutorialCollection(newUser);
        return newUser;
    }
}
