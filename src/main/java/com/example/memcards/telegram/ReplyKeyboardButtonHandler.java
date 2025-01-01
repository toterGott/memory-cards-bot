package com.example.memcards.telegram;

import static com.example.memcards.user.UserState.EVALUATE_ANSWER;
import static com.example.memcards.user.UserState.QUESTION_SHOWED;
import static com.example.memcards.user.UserState.STAND_BY;
import static com.example.memcards.user.UserState.WAITING_FOR_QUESTION;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.example.memcards.card.Card;
import com.example.memcards.card.CardService;
import com.example.memcards.collection.CollectionService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.TelegramUser;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplyKeyboardButtonHandler {

    private final KeyboardProvider keyboardProvider;
    private final MessageProvider messageProvider;
    private final TelegramClientWrapper client;
    private final CollectionService collectionService;
    private final CardService cardService;

    public void handleButton(Update update, TelegramUser user) {
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
                    MINUTES.name()
                );
            }
            case 1 -> {
                appearTime = appearTime.plus(10, MINUTES);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "10",
                    MINUTES.name()
                );
            }
            case 2 -> {
                appearTime = appearTime.plus(1, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    DAYS.name()
                );
            }
            case 3 -> {
                appearTime = appearTime.plus(4, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "4",
                    DAYS.name()
                );
            }
        }

        if (user.getPayload().getFocusOnCollection() != null) {
            var collectionName = collectionService.getById(user.getPayload().getFocusOnCollection()).orElseThrow()
                .getName();
            text += " " + messageProvider.getMessage("collections.focus_on", user.getLanguage(), collectionName);
        }

        card.setAppearTime(appearTime);
        user.setState(STAND_BY);
        var mainMenu = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, text, mainMenu);
    }


    private void getCard(TelegramUser user) {
        var focusedOnCollectionId = user.getPayload().getFocusOnCollection();
        if (focusedOnCollectionId == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(user, messageProvider.getMessage("no_cards", user.getLanguage()))
            );
        } else {
            var collectionName = collectionService.getById(focusedOnCollectionId).orElseThrow().getName();
            cardService.getCardToLearn(user.getId(), focusedOnCollectionId).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(user,
                                         messageProvider.getMessage(
                                             "no_cards_for_focus",
                                             user.getLanguage(),
                                             collectionName
                                         )
                )
            );
        }
    }

    private void sendCard(Card card, TelegramUser user) {
        var keyboard = keyboardProvider.getShowAnswerKeyboard(user.getLanguage());
        client.sendMessage(user, card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);
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

    private void sendSettingsMessage(TelegramUser user) {
        var text = messageProvider.getMessage("settings", user.getLanguage());
        var settingsKeyboard = keyboardProvider.getSettingsMenu();
        client.sendMessage(user, text, settingsKeyboard);
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
}
