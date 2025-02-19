package com.totergott.memcards.scheduler;

import static com.totergott.memcards.telegram.TelegramUtils.telegramUserThreadLocal;
import static java.time.Instant.now;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.telegram.callback.handler.GetCardHandler;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserService;
import jakarta.transaction.Transactional;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSenderScheduler {

    private final CardService cardService;
    private final UserService userService;
    private final GetCardHandler getCardHandler;

    private static final int SAFE_LAG_AMOUNT = 2;
    private static final ChronoUnit SAFE_LAG_UNIT = ChronoUnit.MINUTES;

    private static final int POSTPONE_AMOUNT = SAFE_LAG_AMOUNT;
    private static final ChronoUnit POSTPONE_UNIT = SAFE_LAG_UNIT;

    @Scheduled(cron = "${app.scheduler}")
    @Transactional
    public void schedule() {
        userService.getScheduledUser().ifPresentOrElse(
            user -> cardService.getCardToLearn(user).ifPresentOrElse(
                card -> {
                    telegramUserThreadLocal.set(user);
                    var lastInteraction = user.getPayload().getLastInteractionTimestamp();
                    var safeLag = now().minus(SAFE_LAG_AMOUNT, SAFE_LAG_UNIT);
                    if (lastInteraction.isAfter(safeLag)) {
                        var postpone = now().plus(POSTPONE_AMOUNT, POSTPONE_UNIT);
                        user.getPayload().getSchedule().setNextRun(postpone);
                        log.debug("Card sent is postponed for user {}", user.getUsername());
                        return;
                    }
                    sendCard(card, user);
                    user.getPayload().setLastInteractionTimestamp(now());
                    log.debug("Card sent by schedule to user {}", user.getUsername());
                },
                () -> log.debug("No cards to learn found for user {}", user.getUsername())
            ),
            () -> log.debug("No user with schedule found")
        );
    }

    private void sendCard(Card card, TelegramUser user) {
        getCardHandler.sendCard(card, user);
    }
}
