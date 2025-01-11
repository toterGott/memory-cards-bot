package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.COLLECTION_CREATION;
import static com.totergott.memcards.user.UserState.EVALUATE_ANSWER;
import static com.totergott.memcards.user.UserState.FILL_CARD_QUESTION;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

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
        var key = messageProvider.resolveCode(text);
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.schedule" -> handleSchedule();
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> handleCollectionsButton(user);
            case "button.create_card" -> createCard(user);
            case "button.create_collection" -> createCollection();
            case "button.get_card" -> getCard(user);
            case "button.again" -> setCardGrade(user, update, 0);
            case "button.hard" -> setCardGrade(user, update, 1);
            case "button.good" -> setCardGrade(user, update, 2);
            case "button.easy" -> setCardGrade(user, update, 3);
            case "button.show_answer" -> showAnswer(user);
            case "button.remove_focus" -> removeFocus(user);
            default -> handleUnknownMessage(user, text);
        }
    }

    private void handleSchedule() {
        var schedule = getUser().getPayload().getSchedule();
        String text;
        if (schedule != null) {
            text = messageProvider.getText("schedule.enabled", schedule.getHours().toString());
        } else {
            text = messageProvider.getText("schedule");
        }
        var keyboard = keyboardProvider.getScheduleKeyboard();
        client.sendMessage(text, keyboard);
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

        // todo improve
        switch (grade) {
            default -> {
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    MINUTES.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 1 -> {
                appearTime = appearTime.plus(10, MINUTES);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "10",
                    MINUTES.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 2 -> {
                appearTime = appearTime.plus(1, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    DAYS.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 3 -> {
                appearTime = appearTime.plus(4, DAYS);
                text = messageProvider.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "4",
                    DAYS.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
        }

        if (user.getFocusedOnCollection() != null) {
            var collectionName = collectionService.getById(user.getFocusedOnCollection().getId()).orElseThrow()
                .getName();
            text += " " + messageProvider.getMessage("collections.focus_on", user.getLanguage(), collectionName);
        }

        card.setAppearTime(appearTime);
        user.setState(STAND_BY);
        var mainMenu = keyboardProvider.getMainMenu(user);
        var message = client.sendMessage(user, text, mainMenu);

        if (user.getPayload().getSchedule() != null) {
            var schedule = user.getPayload().getSchedule();
          var nextRun = schedule.getNextRun().plus(schedule.getHours(), ChronoUnit.HOURS);
            schedule.setNextRun(nextRun);
        }

        InlineKeyboardMarkup keyboard = keyboardProvider.getAfterCardAnswer(card.getId());
        text = messageProvider.getText("card.actions");
        client.sendMessage(text, keyboard);
    }

    private void getCard(TelegramUser user) {
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(user, messageProvider.getMessage("no_cards", user.getLanguage()))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(
                    user,
                    messageProvider.getMessage(
                        "no_cards_for_focus",
                        user.getLanguage(),
                        user.getFocusedOnCollection().getName()
                    )
                )
            );
        }
    }

    private void sendCard(Card card, TelegramUser user) {
        var keyboard = keyboardProvider.getShowAnswerKeyboard();
        client.sendMessage(user, card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);
    }

    private void removeFocus(TelegramUser user) {
        user.setFocusedOnCollection(null);
        var keyboard = keyboardProvider.getMainMenu(user);
        client.sendMessage(user, messageProvider.getMessage("focus_removed", user.getLanguage()), keyboard);
    }

    private void handleCollectionsButton(TelegramUser user) {
        var page = collectionService.getCollectionsPage(user.getId(), 0);
        var text = messageProvider.getMessage(
            "collections",
            user.getLanguage()
        );
        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = keyboardProvider.buildPage(page, callback);
        text = messageProvider.appendPageInfo(text, page);
        client.sendMessage(user, text, pageKeyboard);
    }

    private void sendSettingsMessage(TelegramUser user) {
        var text = messageProvider.getText("settings");
        var settingsKeyboard = keyboardProvider.getSettingsMenu(user);
        client.sendMessage(user, text, settingsKeyboard);
    }

    private void handleUnknownMessage(TelegramUser user, String key) {
        getUser().setState(STAND_BY);
        var text = messageProvider.getMessage("unknown_request", user.getLanguage(), key);
        client.sendMessage(user, text, keyboardProvider.getMainMenu(user));
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        var text = messageProvider.getText("collections.create");
        client.sendMessage(text, new ReplyKeyboardRemove(true));
    }

    private void createCard(TelegramUser user) {
        user.setState(FILL_CARD_QUESTION);
        client.sendMessage(
            user,
            messageProvider.getMessage("create_card.question", user.getLanguage()),
            keyboardProvider.hideKeyboard()
        );
    }
}
