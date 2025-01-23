package com.totergott.memcards.telegram.callback;

import static com.totergott.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;

import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback;
import com.totergott.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction;
import com.totergott.memcards.telegram.callback.model.GetCardCallback;
import com.totergott.memcards.telegram.callback.model.GetCardCallback.GetCardCallbackAction;
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
                .action(CollectionCallbackAction.fromCode(callbackArgs[1]))
                .data(callbackArgs[2])
                .additionalData(callbackArgs[3])
                .build();
            case SETTINGS -> SettingsCallback.builder()
                .action(SettingsCallbackAction.fromCode(callbackArgs[1]))
                .data(callbackArgs[2])
                .build();
            case NEW_CARD -> CreateEditCardCallback.builder()
               .action(CreateEditCardCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
                .additionalData(callbackArgs[3])
               .build();
            case GET_CARD -> GetCardCallback.builder()
               .action(GetCardCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .additionalData(callbackArgs[3])
               .build();
            case SCHEDULE -> ScheduleCallback.builder()
               .source(source)
               .action(ScheduleCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .build();
            case PAGE_NAVIGATION -> PageNavigationCallback.builder()
               .action(PageNavigationCallbackAction.fromCode(callbackArgs[1]))
               .data(callbackArgs[2])
               .build();
            default -> throw new IllegalArgumentException("Unhandled callback action: " + source);
        };
        log.debug("Callback: {}", callback.toString());
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
