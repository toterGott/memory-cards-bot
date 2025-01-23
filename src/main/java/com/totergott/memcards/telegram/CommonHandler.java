package com.totergott.memcards.telegram;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static com.totergott.memcards.user.UserState.STAND_BY;

import com.totergott.memcards.card.CardService;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// todo rename
public class CommonHandler {

    private final KeyboardProvider keyboardProvider;
    private final TextProvider textProvider;
    private final MessageService messageService;
    private final CollectionService collectionService;
    private final CardService cardService;

    public void collectionsScreen() {
        messageService.sendMessage(
            textProvider.get("emoji.collection"),
            keyboardProvider.getBackToMainMenuReply()
        );
        var page = collectionService.getCollectionsPage(getUser().getId(), 0);
        var text = textProvider.get("collections");

        CollectionsCallback callback = new CollectionsCallback();
        callback.setAction(CollectionCallbackAction.SELECT);
        var pageKeyboard = keyboardProvider.buildPage(page, callback);
        text = textProvider.appendPageInfo(text, page);
        messageService.sendMessage(text, pageKeyboard);
        messageService.deleteMessagesExceptLast(2);
    }

    public String getDiff(Instant appearTime, Instant now) {
        Duration duration = Duration.between(appearTime, now);
        if (duration.isNegative()) {
            duration = duration.negated();
        }

        long totalSeconds = duration.getSeconds();

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

    public void mainMenu() {
        messageService.checkoutMainMenu();
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

}
