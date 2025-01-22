package com.totergott.memcards.telegram.callback;

import static com.totergott.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;

import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CardCallback;
import com.totergott.memcards.telegram.callback.model.CardCallback.CardCallbackAction;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.NewCardCallback;
import com.totergott.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback;
import com.totergott.memcards.telegram.callback.model.PageNavigationCallback.PageNavigationCallbackAction;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback.ScheduleCallbackAction;
import com.totergott.memcards.telegram.callback.model.SettingsCallback;
import com.totergott.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallbackMapper {

    public static Callback readCallback(String rawCallback) {
        var callbackArgs = rawCallback.split(CALLBACK_DELIMITER);
        var source = CallbackSource.fromCode(callbackArgs[0]);
        var callback = switch (source) {
            case COLLECTIONS -> CollectionsCallback.builder()
                .source(source)
                .action(CollectionCallbackAction.fromCode(callbackArgs[1]))
                .data(callbackArgs[2])
                .additionalData(callbackArgs[3])
                .build();
            case SETTINGS -> SettingsCallback.builder()
                .source(source)
                .action(SettingsCallbackAction.fromCode(callbackArgs[1]))
                .data(callbackArgs[2])
                .build();
            case NEW_CARD -> NewCardCallback.builder()
               .source(source)
               .action(NewCardCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .build();
            case CARD -> CardCallback.builder()
               .source(source)
               .action(CardCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .additionalData(callbackArgs[3])
               .build();
            case SCHEDULE -> ScheduleCallback.builder()
               .source(source)
               .action(ScheduleCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .build();
            case PAGE_NAVIGATION -> PageNavigationCallback.builder()
               .source(source)
               .action(PageNavigationCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .build();
            default -> throw new IllegalArgumentException("Unhandled callback action: " + source);
        };
        log.debug("Callback: {}", callback);
        return callback;
    }

    public static String writeCallback(Callback callback) {
        return callback.getSource().getCode()
            + CALLBACK_DELIMITER
            + callback.getActionCode()
            + CALLBACK_DELIMITER
            + callback.getData()
            + CALLBACK_DELIMITER
            + callback.getAdditionalData();
    }
}
