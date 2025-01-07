package com.example.memcards.scheduler;

import static com.example.memcards.telegram.TelegramUtils.telegramUserThreadLocal;
import static com.example.memcards.telegram.TelegramUtils.updateThreadLocal;
import static com.example.memcards.user.UserState.QUESTION_SHOWED;
import static com.example.memcards.user.UserState.STAND_BY;

import com.example.memcards.card.Card;
import com.example.memcards.card.CardService;
import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.telegram.KeyboardProvider;
import com.example.memcards.telegram.TelegramClientWrapper;
import com.example.memcards.user.TelegramUser;
import com.example.memcards.user.UserService;
import jakarta.transaction.Transactional;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSenderScheduler {

    private final CardService cardService;
    private final UserService userService;
    private final TelegramClientWrapper client;
    private final KeyboardProvider keyboardProvider;
    private final MessageProvider messageProvider;

    @Scheduled(cron = "0/5 * * * * ?")
    @Transactional
    public void schedule() {
        userService.getScheduledUser().ifPresentOrElse(
            user -> cardService.getCardToLearn(user).ifPresentOrElse(
                card -> {
                    telegramUserThreadLocal.set(user);
                    sendCard(card, user);
                    log.info("Card sent by schedule to user {}", user.getUsername());
                },
                () -> log.info("No cards to learn found for user {}", user.getUsername())
            ),
            () -> log.debug("No user with schedule found")
        );
    }

    // todo reuse this method
    private void sendCard(Card card, TelegramUser user) {
        var keyboard = keyboardProvider.getShowAnswerKeyboard();
        client.sendMessage(user, card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);
    }
}
