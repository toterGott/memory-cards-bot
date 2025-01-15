package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getChatId;
import static com.totergott.memcards.telegram.TelegramUtils.getMessage;
import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.COLLECTION_CREATION;
import static com.totergott.memcards.user.UserState.EVALUATE_ANSWER;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.handler.NewCardActionsHandler;
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

    @Value("${app.version}")
    private String version;

    private final KeyboardProvider kp;
    private final MessageProvider messageProvider;
    private final MessageService messageService;
    private final CollectionService collectionService;
    private final CardService cardService;
    private final NewCardActionsHandler newCardActionsHandler;

    public void handleButton(Update update, TelegramUser user) {
        var text = update.getMessage().getText();
        var key = messageProvider.resolveCode(text);
        if (key == null) {
            key = "Key not found";
        }

        switch (key) {
            case "button.schedule" -> handleSchedule();
            case "button.settings" -> sendSettingsMessage(user);
            case "button.collections" -> collections();
            case "button.new_card" -> newCardActionsHandler.startCreateCardDialog();
            case "button.create_collection" -> createCollection();
            case "button.get_card" -> getCard(user);
            case "button.show_answer" -> showAnswer(user);
            case "button.remove_focus" -> removeFocus(user);
            case "button.back_to_main_menu" -> mainMenu();
            default -> handleUnknownMessage();
        }
    }

    private void mainMenu() {
        messageService.sendMessage(
            messageProvider.getText("main_menu"),
            kp.getMainMenu()
        );
        messageService.deleteMessagesExceptLast(1);
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
        var schedule = getUser().getPayload().getSchedule();
        String text;
        if (schedule != null) {
            text = messageProvider.getText("schedule.enabled", schedule.getHours().toString());
        } else {
            text = messageProvider.getText("schedule");
        }
        var keyboard = kp.getScheduleKeyboard();
        messageService.sendMessage(text, keyboard);
    }

    private void showAnswer(TelegramUser user) {
        var card = cardService.getCard(user.getCurrentCardId());
        var keyboard = kp.getKnowledgeCheckKeyboard(user.getLanguage());
        user.setState(EVALUATE_ANSWER);
        messageService.sendMessage( card.getAnswer(), keyboard);
    }

    private void setCardGrade(TelegramUser user, Update update, int grade) {
        var card = cardService.getCard(user.getCurrentCardId());

        String text;
        Instant appearTime = Instant.now();

        // todo improve, get rid of bulky switch, simplify
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
        var mainMenu = kp.getMainMenu(user);
        messageService.sendMessage( text, mainMenu);

        if (user.getPayload().getSchedule() != null) {
            var schedule = user.getPayload().getSchedule();
            var nextRun = schedule.getNextRun().plus(schedule.getHours(), ChronoUnit.HOURS);
            schedule.setNextRun(nextRun);
        }

        InlineKeyboardMarkup keyboard = kp.getCardMenuAfterAnswer(card.getId());
        text = messageProvider.getText("card.actions");
        messageService.sendMessage(text, keyboard);
    }

    private void getCard(TelegramUser user) {
        if (user.getFocusedOnCollection() == null) {
            cardService.getCardToLearn(user.getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(messageProvider.getMessage("no_cards", user.getLanguage()))
            );
        } else {
            cardService.getCardToLearn(user.getId(), user.getFocusedOnCollection().getId()).ifPresentOrElse(
                card -> sendCard(card, user),
                () -> messageService.sendMessage(
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
        messageService.sendMessage(messageProvider.getText("card_placeholder"));
        var collectionName = card.getCollection().getName();
        messageService.sendMessage(
            messageProvider.getText("button.collections.emoji") + collectionName,
            kp.getOneReplyButton(messageProvider.getText("button.back_to_main_menu"))
        );

        var keyboard = kp.getInlineShowAnswerKeyboard(card.getId());
        messageService.sendMessage(messageProvider.getText("button.card.question_emoji") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(3);
    }

    private void removeFocus(TelegramUser user) {
        user.setFocusedOnCollection(null);
        var keyboard = kp.getMainMenu(user);
        messageService.sendMessage( messageProvider.getMessage("focus_removed", user.getLanguage()), keyboard);
    }

    private void collections() {
        messageService.sendMessage(
            messageProvider.getText("button.collections.emoji"),
            kp.getOneReplyButton(messageProvider.getText("button.back_to_main_menu"))
        );
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = messageProvider.getText("collections");

        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = kp.buildPage(page, callback);
        text = messageProvider.appendPageInfo(text, page);
        messageService.sendMessage(text, pageKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    private void sendSettingsMessage(TelegramUser user) {
        messageService.sendMessage(
            messageProvider.getText("emoji.settings"),
            kp.getOneReplyButton(messageProvider.getText("button.back_to_main_menu"))
        );
        var text = "v." + version + "\n" + messageProvider.getText("settings");
        var settingsKeyboard = kp.getSettingsMenu(user);
        messageService.sendMessage(text, settingsKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    private void handleUnknownMessage() {
        messageService.deleteMessage(getChatId(), getMessage().getMessageId());
    }

    private void createCollection() {
        getUser().setState(COLLECTION_CREATION);
        var text = messageProvider.getText("collections.create");
        messageService.sendMessage(text, new ReplyKeyboardRemove(true));
    }
}
