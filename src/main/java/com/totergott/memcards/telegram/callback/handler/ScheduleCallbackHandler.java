package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;

import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.TelegramUser.Payload.Schedule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCallbackHandler implements CallbackHandler {

    private final TextProvider textProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService client;

    @Getter
    CallbackSource callbackSource = CallbackSource.SCHEDULE;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        ScheduleCallback scheduleCallback = (ScheduleCallback) callback;
        switch (scheduleCallback.getAction()) {
            case DISABLE -> disable();
            case SET_TIME -> enableSchelling(scheduleCallback.getData());
        }
    }

    private void disable() {
        getUser().getPayload().setSchedule(null);

        var text = textProvider.get("schedule.disabled");
        var keyboard = keyboardProvider.getScheduleKeyboard();
        client.deleteMessagesExceptFirst(1);
        client.sendMessage(text, keyboard);
    }

    private void enableSchelling(String data) {
        var schedule = new Schedule();
        var hours = Integer.parseInt(data);
        schedule.setHours(hours);
        schedule.setNextRun(Instant.now().plus(hours, ChronoUnit.HOURS));
        getUser().getPayload().setSchedule(schedule);

        var text = textProvider.get("schedule.enabled", data);
        var keyboard = keyboardProvider.getScheduleKeyboard();
        client.deleteMessagesExceptFirst(1);
        client.sendMessage(text, keyboard);
    }
}
