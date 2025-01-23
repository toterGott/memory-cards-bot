package com.totergott.memcards.scheduler;

import static com.totergott.memcards.telegram.TelegramUtils.telegramUserThreadLocal;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.telegram.callback.handler.GetCardHandler;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
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

    @Scheduled(cron = "0/5 * * * * ?")
    @Transactional
    public void schedule() {
        userService.getScheduledUser().ifPresentOrElse(
            user -> cardService.getCardToLearn(user).ifPresentOrElse(
                card -> {
                    telegramUserThreadLocal.set(user);
                    var lastInteraction = user.getPayload().getLastInteractionTimestamp();
                    var safeLag = Instant.now().minus(1, ChronoUnit.MINUTES);
                    if (lastInteraction.isAfter(safeLag)) {
                        var postpone = Instant.now().plus(1, ChronoUnit.MINUTES);
                        user.getPayload().getSchedule().setNextRun(postpone);
                        log.info("Card sent is postponed for user {}", user.getUsername());
                        return;
                    }
                    sendCard(card, user);
                    log.info("Card sent by schedule to user {}", user.getUsername());
                },
                () -> log.info("No cards to learn found for user {}", user.getUsername())
            ),
            () -> log.debug("No user with schedule found")
        );
    }

    private void sendCard(Card card, TelegramUser user) {
        getCardHandler.sendCard(card, user);
    }
}
