package com.example.memcards.telegram;

import static com.example.memcards.user.UserState.STAND_BY;

import com.example.memcards.card.Card;
import com.example.memcards.card.CardService;
import com.example.memcards.collection.CardCollection;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.TelegramUser;
import com.example.memcards.user.UserService;
import com.example.memcards.user.UserState;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateHandler {

    private final TelegramClientWrapper client;
    private final UserService userService;
    private final KeyboardProvider keyboardProvider;
    private final MessageProvider messageProvider;
    private final CollectionService collectionService;
    private final CardService cardService;
    private final TelegramCallbackHandler callbackHandler;
    private final ReplyKeyboardButtonHandler buttonHandler;

    public static final String COMMAND_TYPE = "bot_command";

    @Transactional
    public void handleUpdate(Update update) {
        var user = welcomeOrGetUser(update);
        if (update.hasCallbackQuery()) {
            callbackHandler.handleCallback(update, user);
        } else if (update.hasMessage()) {
            handleUpdateByUserState(update, user);
        }
    }

    private void handleUpdateByUserState(Update update, TelegramUser user) {
        switch (user.getState()) {
            case STAND_BY -> handleStandBy(update, user);
            case WAITING_FOR_QUESTION -> handleWaitingForQuestion(update, user);
            case WAITING_FOR_ANSWER -> handleWaitingForAnswer(update, user);
            case QUESTION_SHOWED, EVALUATE_ANSWER -> buttonHandler.handleButton(update, user);
        }
    }

    private void handleStandBy(Update update, TelegramUser user) {
        if (update.getMessage().getEntities() != null) {
            var command = update.getMessage().getEntities().stream()
                .filter(messageEntity -> messageEntity.getType().equals(COMMAND_TYPE))
                .findFirst();
            if (command.isPresent()) {
                handleCommand(command.get(), user);
                return;
            }
        }

        buttonHandler.handleButton(update, user);
    }

    private void handleWaitingForAnswer(Update update, TelegramUser user) {
        Card card = cardService.getCard(user.getCurrentCardId());
        card.setAnswer(update.getMessage().getText());

        user.setState(STAND_BY);
        user.setCurrentCardId(null);
        userService.save(user);

        client.sendMessage(user, messageProvider.getMessage("create_card.created", user.getLanguage()));
        client.sendMessage(user, card.getQuestion());
        client.sendMessage(user, card.getAnswer(), keyboardProvider.getMainMenu(user));
    }

    private void handleWaitingForQuestion(Update update, TelegramUser user) {
        CardCollection collection;
        if (user.getPayload().getFocusOnCollection() != null) {
            collection = collectionService.getById(user.getPayload().getFocusOnCollection()).orElseThrow();
        } else {
            collection = collectionService.getById(user.getPayload().getDefaultCollection()).orElseThrow();
        }

        var card = new Card();
        card.setQuestion(update.getMessage().getText());
        card.setCollection(collection);
        card.setOwner(user);
        card.setAppearTime(Instant.now());
        card = cardService.save(card);

        user.setState(UserState.WAITING_FOR_ANSWER);
        user.setCurrentCardId(card.getId());

        client.sendMessage(user, messageProvider.getMessage("create_card.answer", user.getLanguage()));
    }

    private void handleCommand(MessageEntity messageEntity, TelegramUser user) {
        switch (messageEntity.getText()) {
            case "/start" -> {
                user.setState(STAND_BY);
                userService.save(user);
                sendWelcomeMessage(user);
            }
        }
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

        sendWelcomeMessage(newUser);
        collectionService.initDefaultCollection(newUser);
        return newUser;
    }

    private void sendWelcomeMessage(TelegramUser user) {
        var welcomeText = messageProvider.getMessage("welcome", user.getLanguage());
        var mainMenu = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, welcomeText, mainMenu);
    }
}
