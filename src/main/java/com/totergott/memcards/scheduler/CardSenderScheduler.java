package com.totergott.memcards.scheduler;

import static com.totergott.memcards.telegram.TelegramUtils.telegramUserThreadLocal;
import static com.totergott.memcards.user.UserState.QUESTION_SHOWED;

import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.UserService;
import jakarta.transaction.Transactional;
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
    private final MessageService client;
    private final KeyboardProvider keyboardProvider;
    private final TextProvider textProvider;

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
        client.sendMessage(card.getQuestion(), keyboard);
        user.setCurrentCardId(card.getId());
        user.setState(QUESTION_SHOWED);
    }
}
