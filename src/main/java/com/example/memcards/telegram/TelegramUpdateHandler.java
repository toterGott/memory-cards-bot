package com.example.memcards.telegram;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;
import static com.example.memcards.user.UserState.EVALUATE_ANSWER;
import static com.example.memcards.user.UserState.QUESTION_SHOWED;
import static com.example.memcards.user.UserState.STAND_BY;
import static com.example.memcards.user.UserState.WAITING_FOR_QUESTION;

import com.example.memcards.card.Card;
import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.AvailableLocale;
import com.example.memcards.user.TelegramUser;
import com.example.memcards.user.UserService;
import com.example.memcards.user.UserState;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
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

    public static final String COMMAND_TYPE = "bot_command";

    @Transactional
    public void handleUpdate(Update update) {
        var user = welcomeOrGetUser(update);
        if (update.hasCallbackQuery()) {
            handleCallback(update, user);
        } else if (update.hasMessage()) {
            handleUpdateByUserState(update, user);
        }
    }

    private void handleCallback(Update update, TelegramUser user) {
        var callbackArgs = update.getCallbackQuery().getData().split(CALLBACK_DELIMITER);
        var callbackAction = CallbackAction.valueOf(callbackArgs[0]);

        switch (callbackAction) {
            case SET_LANGUAGE -> handleLanguageChangeCallback(callbackArgs[1], user, update.getCallbackQuery().getId());
        }
    }

    private void handleUpdateByUserState(Update update, TelegramUser user) {
        switch (user.getState()) {
            case STAND_BY -> handleStandBy(update, user);
            case WAITING_FOR_QUESTION -> handleWaitingForQuestion(update, user);
            case WAITING_FOR_ANSWER -> handleWaitingForAnswer(update, user);
            case QUESTION_SHOWED, EVALUATE_ANSWER -> handleButton(update, user);
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

        handleButton(update, user);
    }

    private void handleLanguageChangeCallback(String localeCode, TelegramUser user, String callbackId) {
        var locale = AvailableLocale.valueOf(localeCode);
        user.setLanguage(locale);

        client.sendMessage(
            user.getChatId(),
            messageProvider.getMessage(
                "language.updated",
                user.getLanguage(),
                locale.getName()
            ),
            keyboardProvider.getMainMenu(user.getLanguage())
        );

        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        answer.setShowAlert(false);
        client.execute(answer);
    }

    private void handleWaitingForAnswer(Update update, TelegramUser user) {
        Card card = cardService.getCard(user.getCurrentCardId());
        card.setAnswer(update.getMessage().getText());

        user.setState(STAND_BY);
        user.setCurrentCardId(null);
        userService.save(user);

        client.sendMessage(user.getChatId(), messageProvider.getMessage("create_card.created", user.getLanguage()));
        client.sendMessage(user.getChatId(), card.getQuestion());
        client.sendMessage(user.getChatId(), card.getAnswer(), keyboardProvider.getMainMenu(user.getLanguage()));
    }

    private void handleWaitingForQuestion(Update update, TelegramUser user) {
        var defaultCollection = collectionService.getDefaultCollection(user.getId());
        var card = new Card();
        card.setQuestion(update.getMessage().getText());
        card.setCollection(defaultCollection);
        card.setOwner(user);
        card.setGrade(0);
        card = cardService.save(card);

        user.setState(UserState.WAITING_FOR_ANSWER);
        user.setCurrentCardId(card.getId());

        client.sendMessage(user.getChatId(), messageProvider.getMessage("create_card.answer", user.getLanguage()));
    }

    private void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = messageProvider.resolveCode(text, user.getLanguage());
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.info" -> client.sendMessage(
                user.getChatId(), messageProvider.getMessage("info", user.getLanguage()),
                keyboardProvider.getMainMenu(user.getLanguage())
            );
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" ->
                client.sendMessage(user.getChatId(), messageProvider.getMessage("collections", user.getLanguage()));
            case "button.create_card" -> createCard(user);
            case "button.get_card" -> getCard(user);
            case "button.again" -> setCardGrade(user, update, 0);
            case "button.hard" -> setCardGrade(user, update, 1);
            case "button.good" -> setCardGrade(user, update, 2);
            case "button.easy" -> setCardGrade(user, update, 3);
            case "button.show_answer" -> showAnswer(user);
            default -> handleUnknownMessage(user, key);
        }
    }

    private void showAnswer(TelegramUser user) {
        var card = cardService.getCard(user.getCurrentCardId());
        var keyboard = keyboardProvider.getKnowledgeCheckKeyboard(user.getLanguage());
        user.setState(EVALUATE_ANSWER);
        client.sendMessage(user.getChatId(), card.getAnswer(), keyboard);
    }

    private void setCardGrade(TelegramUser user, Update update, int i) {
        var card = cardService.getCard(user.getCurrentCardId());
        card.setGrade(i);
        card.setUpdatedAt(LocalDateTime.now());
        user.setState(STAND_BY);
        var mainMenu = keyboardProvider.getMainMenu(user.getLanguage());
        client.sendMessage(user.getChatId(), messageProvider.getMessage("card_answered", user.getLanguage()), mainMenu);
    }

    private void getCard(TelegramUser user) {
        cardService.getCardToLearn(user.getId()).ifPresentOrElse(
            card -> {
                var keyboard = keyboardProvider.getShowAnswerKeyboard(user.getLanguage());
                client.sendMessage(user.getChatId(), card.getQuestion(), keyboard);
                user.setCurrentCardId(card.getId());
                user.setState(QUESTION_SHOWED);
            },
            () -> client.sendMessage(user.getChatId(), messageProvider.getMessage("no_cards", user.getLanguage()))
        );
    }

    private void handleUnknownMessage(TelegramUser user, String key) {
        var text = messageProvider.getMessage("unknown_request", user.getLanguage(), key);
        client.sendMessage(user.getChatId(), text + key, keyboardProvider.getMainMenu(user.getLanguage()));
    }

    private void createCard(TelegramUser user) {
        user.setState(WAITING_FOR_QUESTION);
        client.sendMessage(
            user.getChatId(),
            messageProvider.getMessage("create_card.question", user.getLanguage()),
            keyboardProvider.hideKeyboard()
        );
    }

    private void sendSettingsMessage(TelegramUser user) {
        var text = messageProvider.getMessage("settings", user.getLanguage());
        var settingsKeyboard = keyboardProvider.getSettingsMenu();
        client.sendMessage(user.getChatId(), text, settingsKeyboard);
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
        var mainMenu = keyboardProvider.getMainMenu(user.getLanguage());
        client.sendMessage(user.getChatId(), welcomeText, mainMenu);
    }
}
