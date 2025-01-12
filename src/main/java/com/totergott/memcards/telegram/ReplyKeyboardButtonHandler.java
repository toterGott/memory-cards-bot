package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getChatId;
import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReplyKeyboardButtonHandler {

    private final MessageService messageService;
    @Value("${app.version}")
    private String version;

    private final KeyboardProvider kp;
    private final MessageProvider mp;
    private final MessageService client;
    private final CollectionService collectionService;
    private final CardService cardService;

    public void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = mp.resolveCode(text);
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.schedule" -> handleSchedule();
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> collections();
            case "button.create_card" -> createCard(user);
            case "button.create_collection" -> createCollection();
            case "button.get_card" -> getCard(user);
            case "button.again" -> setCardGrade(user, update, 0);
            case "button.hard" -> setCardGrade(user, update, 1);
            case "button.good" -> setCardGrade(user, update, 2);
            case "button.easy" -> setCardGrade(user, update, 3);
            case "button.show_answer" -> showAnswer(user);
            case "button.remove_focus" -> removeFocus(user);
            case "button.back_to_main_menu" -> mainMenu();
            default -> handleUnknownMessage(user, text);
        }
    }

    private void mainMenu() {
        client.sendMessage(
            mp.getText("main_menu"),
            kp.getMainMenu()
        );
        client.deleteMessagesExceptLast(1);
    }

    private void handleSchedule() {
        var schedule = getUser().getPayload().getSchedule();
        String text;
        if (schedule != null) {
            text = mp.getText("schedule.enabled", schedule.getHours().toString());
        } else {
            text = mp.getText("schedule");
        }
        var keyboard = kp.getScheduleKeyboard();
        client.sendMessage(text, keyboard);
    }

    private void showAnswer(TelegramUser user) {
        var card = cardService.getCard(user.getCurrentCardId());
        var keyboard = kp.getKnowledgeCheckKeyboard(user.getLanguage());
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
                text = mp.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    MINUTES.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 1 -> {
                appearTime = appearTime.plus(10, MINUTES);
                text = mp.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "10",
                    MINUTES.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 2 -> {
                appearTime = appearTime.plus(1, DAYS);
                text = mp.getMessage(
                    "card_answered",
                    user.getLanguage(),
                    "1",
                    DAYS.toString().toLowerCase(),
                    card.getCollection().getName()
                );
            }
            case 3 -> {
                appearTime = appearTime.plus(4, DAYS);
                text = mp.getMessage(
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
            text += " " + mp.getMessage("collections.focus_on", user.getLanguage(), collectionName);
        }

        card.setAppearTime(appearTime);
        user.setState(STAND_BY);
        var mainMenu = kp.getMainMenu(user);
        client.sendMessage(user, text, mainMenu);

        if (user.getPayload().getSchedule() != null) {
            var schedule = user.getPayload().getSchedule();
          var nextRun = schedule.getNextRun().plus(schedule.getHours(), ChronoUnit.HOURS);
            schedule.setNextRun(nextRun);
        }

        InlineKeyboardMarkup keyboard = kp.getCardMenuAfterAnswer(card.getId());
        text = mp.getText("card.actions");
        client.sendMessage(text, keyboard);
    }

    private void getCard(TelegramUser user) {
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(user, mp.getMessage("no_cards", user.getLanguage()))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> client.sendMessage(
                    user,
                    mp.getMessage(
                        "no_cards_for_focus",
                        user.getLanguage(),
                        user.getFocusedOnCollection().getName()
                    )
                )
            );
        }
    }

    private void sendCard(Card card, TelegramUser user) {
        var collectionName = card.getCollection().getName();
        client.sendMessage(
            mp.getText("button.collections.emoji") + collectionName,
            kp.getSingleButton(mp.getText("button.back_to_main_menu"))
        );

        var keyboard = kp.getInlineShowAnswerKeyboard(card.getId());
        client.sendMessage(user, mp.getText("button.card.emoji") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(2);
    }

    private void removeFocus(TelegramUser user) {
        user.setFocusedOnCollection(null);
        var keyboard = kp.getMainMenu(user);
        client.sendMessage(user, mp.getMessage("focus_removed", user.getLanguage()), keyboard);
    }

    private void collections() {
        client.sendMessage(
            mp.getText("button.collections.emoji"),
            kp.getSingleButton(mp.getText("button.back_to_main_menu"))
        );
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = mp.getText("collections");

        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = kp.buildPage(page, callback);
        text = mp.appendPageInfo(text, page);
        client.sendMessage(getUser(), text, pageKeyboard);
        client.deleteMessagesExceptLast(2);
    }

    private void sendSettingsMessage(TelegramUser user) {
        var text = "v." + version + "\n" + mp.getText("settings");
        var settingsKeyboard = kp.getSettingsMenu(user);
        client.sendMessage(user, text, settingsKeyboard);
    }

    private void handleUnknownMessage(TelegramUser user, String key) {
        var text = mp.getMessage("unknown_request", user.getLanguage(), key);
        client.deleteMessage(getChatId(), getMessage().getMessageId());
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        var text = mp.getText("collections.create");
        client.sendMessage(text, new ReplyKeyboardRemove(true));
    }

    private void createCard(TelegramUser user) {
        user.setState(FILL_CARD_QUESTION);
        client.sendMessage(
            user,
            mp.getMessage("create_card.question", user.getLanguage()),
            kp.hideKeyboard()
        );
    }
}
