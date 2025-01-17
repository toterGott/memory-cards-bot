package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.telegram.callback.handler.NewCardActionsHandler;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// todo rename
public class CommonHandler {
    private final KeyboardProvider keyboardProvider;
    private final MessageProvider messageProvider;
    private final MessageService messageService;
    private final CollectionService collectionService;
    private final CardService cardService;
    private final NewCardActionsHandler newCardActionsHandler;

    public void collectionsScreen() {
        messageService.sendMessage(
            messageProvider.getText("emoji.collection"),
            keyboardProvider.getBackToMainMenuReply()
        );
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = messageProvider.getText("collections");

        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = keyboardProvider.buildPage(page, callback);
        text = messageProvider.appendPageInfo(text, page);
        messageService.sendMessage(text, pageKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    public void cardScreen() {
        var user = getUser();
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
        messageService.sendMessage(
            messageProvider.getText("emoji.card"),
            keyboardProvider.getBackToMainMenuReply()
        );

        var now = Instant.now();
        if (card.getAppearTime().isAfter(now)) {
            var diff = getDiff(now, card.getAppearTime());
            messageService.sendMessage(
              messageProvider.getText("no_cards_yet", diff)
            );
            messageService.deleteMessagesExceptLast(2);
            return;
        }
        var collectionName = card.getCollection().getName();
        messageService.sendMessage(messageProvider.getText("emoji.collection") + collectionName);

        var keyboard = keyboardProvider.getInlineShowAnswerKeyboard(card.getId());
        messageService.sendMessage(messageProvider.getText("emoji.card") + card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);

        messageService.deleteMessagesExceptLast(3);
    }

    private String getDiff(Instant appearTime, Instant now) {
        Duration duration = Duration.between(appearTime, now);
        if (duration.isNegative()) {
            duration = duration.negated();
        }

        long totalSeconds = duration.getSeconds();

        // Если разница меньше минуты, показываем только секунды
        if (totalSeconds < 60) {
            return totalSeconds + " " + ChronoUnit.SECONDS;
        }

        long totalMinutes = totalSeconds / 60;
        long days = totalMinutes / (60 * 24);
        totalMinutes %= (60 * 24);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(" ").append(ChronoUnit.DAYS);
        }
        if (hours > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(hours).append(" ").append(ChronoUnit.HOURS);
        }
        if (minutes > 0 || sb.length() == 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(minutes).append(" ").append(ChronoUnit.MINUTES);
        }

        return sb.toString();
    }
}
