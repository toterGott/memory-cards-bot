package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.telegram.TelegramUtils.getUser;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.CommonHandler;
import com.totergott.memcards.telegram.InlineKeyboardBuilder;
import com.totergott.memcards.telegram.KeyboardProvider;
import com.totergott.memcards.telegram.MessageService;
import com.totergott.memcards.telegram.callback.CallbackHandler;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback.ScheduleCallbackAction;
import com.totergott.memcards.user.TelegramUser;
import com.totergott.memcards.user.TelegramUser.Payload.Schedule;
import com.totergott.memcards.user.TelegramUser.Payload.SchedulingOption;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCallbackHandler implements CallbackHandler {

    private final TextProvider textProvider;
    private final KeyboardProvider keyboardProvider;
    private final MessageService messageService;

    private static final Map<Integer, SchedulingOption> SCHEDULING_OPTIONS = Map.of(
        0, new SchedulingOption(MINUTES, 10),
        1, new SchedulingOption(MINUTES, 30),
        2, new SchedulingOption(HOURS, 1),
        3, new SchedulingOption(HOURS, 3),
        4, new SchedulingOption(HOURS, 6),
        5, new SchedulingOption(HOURS, 12)
    );

    private final CommonHandler commonHandler;

    @Getter
    CallbackSource callbackSource = CallbackSource.SCHEDULE;

    @Override
    public void handle(Callback callback, CallbackQuery callbackQuery, TelegramUser user) {
        ScheduleCallback scheduleCallback = (ScheduleCallback) callback;
        switch (scheduleCallback.getAction()) {
            case DISABLE -> disable();
            case SET_TIME -> enableScheduling(scheduleCallback.getData());
            case BACK -> commonHandler.setMainMenu();
        }
    }

    public void handleSchedule() {
        var schedule = getUser().getPayload().getSchedule();
        String text;
        if (schedule != null) {
            text = getScheduleEnabledText(schedule.getOption());
        } else {
            text = textProvider.get("schedule");
        }

        messageService.sendMessage(
            textProvider.get("emoji.schedule"),
            keyboardProvider.getBackToMainMenuReply()
        );
        messageService.sendMessage(text, buildKeyboard());
        messageService.deleteMessagesExceptLast(2);
    }

    private void disable() {
        getUser().getPayload().setSchedule(null);

        var text = textProvider.get("schedule.disabled");
        messageService.deleteMessagesExceptLast(2);
        messageService.editCallbackMessage(text, buildKeyboard());
    }

    private void enableScheduling(String data) {
        var schedule = new Schedule();
        var option = SCHEDULING_OPTIONS.get(Integer.parseInt(data));
        schedule.setOption(option);
        schedule.setNextRun(Instant.now().plus(option.amount(), option.chronoUnit()));
        getUser().getPayload().setSchedule(schedule);

        var text = getScheduleEnabledText(option);
        messageService.deleteMessagesExceptLast(2);
        messageService.editCallbackMessage(text, buildKeyboard());
    }

    private String getScheduleEnabledText(SchedulingOption option) {
        return textProvider.get("schedule.enabled", option.amount() + " " + option.chronoUnit());
    }

    private InlineKeyboardMarkup buildKeyboard() {
        var keyboardBuilder = new InlineKeyboardBuilder();

        for (int i = 0; i < SCHEDULING_OPTIONS.size(); i++) {
            var option = SCHEDULING_OPTIONS.get(i);
            var callbackBuilder = ScheduleCallback.builder()
                .action(ScheduleCallbackAction.SET_TIME)
                .data(String.valueOf(i));
            keyboardBuilder.addButton(
                option.amount() + " " + option.chronoUnit().name().toLowerCase(),
                callbackBuilder.build()
            );
        }

        keyboardBuilder.nextRow();
        var disableCallback = ScheduleCallback.builder()
            .action(ScheduleCallbackAction.DISABLE)
            .build();
        keyboardBuilder.addButton(textProvider.get("button.schedule.settings.disable"), disableCallback);

        var backCallback = ScheduleCallback.builder()
            .action(ScheduleCallbackAction.BACK)
            .build();
        keyboardBuilder.addButton(textProvider.get("emoji.back") + textProvider.get("button.back"), backCallback);

        return keyboardBuilder.build();
    }
}
