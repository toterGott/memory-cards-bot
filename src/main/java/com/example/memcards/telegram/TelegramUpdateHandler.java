package com.example.memcards.telegram;

import static com.example.memcards.user.UserState.EVALUATE_ANSWER;
import static com.example.memcards.user.UserState.QUESTION_SHOWED;
import static com.example.memcards.user.UserState.STAND_BY;
import static com.example.memcards.user.UserState.WAITING_FOR_QUESTION;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

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
import org.springframework.data.domain.PageRequest;
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
    private final ActionDelegate buttonHandler;

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
        var defaultCollection = collectionService.getById(user.getPayload().getDefaultCollection()).orElseThrow();
        var card = new Card();
        card.setQuestion(update.getMessage().getText());
        card.setCollection(defaultCollection);
        card.setOwner(user);
        card.setAppearTime(Instant.now());
        card = cardService.save(card);

        user.setState(UserState.WAITING_FOR_ANSWER);
        user.setCurrentCardId(card.getId());

        client.sendMessage(user, messageProvider.getMessage("create_card.answer", user.getLanguage()));
    }

    private void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = messageProvider.resolveCode(text, user.getLanguage());
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.info" -> client.sendMessage(
                user, messageProvider.getMessage("info", user.getLanguage()),
                keyboardProvider.getMainMenu(user)
            );
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> handleCollectionsButton(user);
            case "button.create_card" -> createCard(user);
            case "button.get_card" -> getCard(user);
            case "button.again" -> setCardGrade(user, update, 0);
            case "button.hard" -> setCardGrade(user, update, 1);
            case "button.good" -> setCardGrade(user, update, 2);
            case "button.easy" -> setCardGrade(user, update, 3);
            case "button.show_answer" -> showAnswer(user);
            case "button.remove_focus" -> removeFocus(user);
            default -> handleUnknownMessage(user, key);
        }
    }

    private void removeFocus(TelegramUser user) {
        user.getPayload().setFocusOnCollection(null);
        var keyboard = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, messageProvider.getMessage("focus_removed", user.getLanguage()), keyboard);
    }

    private void handleCollectionsButton(TelegramUser user) {
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = messageProvider.getMessage(
            "collections",
            user.getLanguage(),
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages())
        );
        var pageKeyboard = keyboardProvider.getCollectionsPageInlineKeyboard(user.getLanguage(), page);
        client.sendMessage(user, text, pageKeyboard);
    }

    private void showAnswer(TelegramUser user) {
        var card = cardService.getCard(user.getCurrentCardId());
        var keyboard = keyboardProvider.getKnowledgeCheckKeyboard(user.getLanguage());
        user.setState(EVALUATE_ANSWER);
        client.sendMessage(user, card.getAnswer(), keyboard);
    }

    private void setCardGrade(TelegramUser user, Update update, int grade) {
        var card = cardService.getCard(user.getCurrentCardId());

        String text;
        Instant appearTime = Instant.now();
        switch (grade) {
            default -> {
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    MINUTES.name(),
                    card.getCollection().getName()
                );
            }
            case 1 -> {
                appearTime = appearTime.plus(10, MINUTES);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "10",
                    MINUTES.name(),
                    card.getCollection().getName()
                );
            }
            case 2 -> {
                appearTime = appearTime.plus(1, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    DAYS.name(),
                    card.getCollection().getName()
                );
            }
            case 3 -> {
                appearTime = appearTime.plus(4, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "4",
                    DAYS.name(),
                    card.getCollection().getName()
                );
            }
        }
        card.setAppearTime(appearTime);
        user.setState(STAND_BY);
        var mainMenu = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, text, mainMenu);
    }

    private void getCard(TelegramUser user) {
        cardService.getCardToLearn(user.getId()).ifPresentOrElse(
            card -> {
                var keyboard = keyboardProvider.getShowAnswerKeyboard(user.getLanguage());
                client.sendMessage(user, card.getQuestion(), keyboard);
                user.setCurrentCardId(card.getId());
                user.setState(QUESTION_SHOWED);
            },
            () -> client.sendMessage(user, messageProvider.getMessage("no_cards", user.getLanguage()))
        );
    }

    private void handleUnknownMessage(TelegramUser user, String key) {
        var text = messageProvider.getMessage("unknown_request", user.getLanguage(), key);
        client.sendMessage(user, text + key, keyboardProvider.getMainMenu(user));
    }

    private void createCard(TelegramUser user) {
        user.setState(WAITING_FOR_QUESTION);
        client.sendMessage(
            user,
            messageProvider.getMessage("create_card.question", user.getLanguage()),
            keyboardProvider.hideKeyboard()
        );
    }

    private void sendSettingsMessage(TelegramUser user) {
        var text = messageProvider.getMessage("settings", user.getLanguage());
        var settingsKeyboard = keyboardProvider.getSettingsMenu();
        client.sendMessage(user, text, settingsKeyboard);
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
