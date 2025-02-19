package com.totergott.memcards.scheduler;

import static com.totergott.memcards.TestUtils.DEFAULT_LANGUAGE_CODE;
import static com.totergott.memcards.TestUtils.getUpdateWithCommand;
import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.collection.CollectionService;
import com.totergott.memcards.user.TelegramUser.Payload.Schedule;
import com.totergott.memcards.user.TelegramUser.Payload.SchedulingOption;
import com.totergott.memcards.user.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootTest
class CardSenderSchedulerTest extends BaseTest {

    @Autowired
    private UserService userService;
    @Autowired
    private CardSenderScheduler cardSenderScheduler;
    @Autowired
    private CollectionService collectionService;

    @Test
    void whenNoUserWithScheduler_thenScheduleTick_thenNoMessagesSent() throws TelegramApiException {
        var update = getUpdateWithCommand(START_COMMAND);
        var user = userService.createUser(update.getMessage().getChat(), DEFAULT_LANGUAGE_CODE);
        userService.save(user);

        cardSenderScheduler.schedule();

        verify(telegramClient, times(0)).execute(any(SendMessage.class));
    }

    // todo test scheduler enabling

    @ParameterizedTest
    @MethodSource("schedulingConditions")
    void whenUserWithScheduler_thenScheduleTick_thenNoMessagesSent(
        Instant lastInteraction,
        VerificationMode verificationMode
    ) throws TelegramApiException {
        var update = getUpdateWithCommand(START_COMMAND);
        var user = userService.createUser(update.getMessage().getChat(), DEFAULT_LANGUAGE_CODE);
        Schedule schedule = new Schedule();
        schedule.setOption(new SchedulingOption(ChronoUnit.MINUTES, 1));
        schedule.setNextRun(Instant.now());
        user.getPayload().setSchedule(schedule);
        user.getPayload().setLastInteractionTimestamp(lastInteraction);
        userService.save(user);
        collectionService.initTutorialCollection(user);

        cardSenderScheduler.schedule();

        verify(telegramClient, verificationMode).execute(any(SendMessage.class));
    }

    public static Stream<Arguments> schedulingConditions() {
        return Stream.of(
            of(
                Instant.now().minus(1, ChronoUnit.MINUTES),
                atLeastOnce()
            ),
            of(
                Instant.now().minus(55, ChronoUnit.SECONDS),
                never()
            )
        );
    }
}