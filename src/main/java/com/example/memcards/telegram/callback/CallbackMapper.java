package com.example.memcards.telegram.callback;

import static com.example.memcards.telegram.TelegramUtils.CALLBACK_DELIMITER;

import com.example.memcards.telegram.callback.model.Callback;
import com.example.memcards.telegram.callback.model.CallbackSource;
import com.example.memcards.telegram.callback.model.CollectionsCallback;
import com.example.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import com.example.memcards.telegram.callback.model.NewCardCallback;
import com.example.memcards.telegram.callback.model.NewCardCallback.NewCardCallbackAction;
import com.example.memcards.telegram.callback.model.SettingsCallback;
import com.example.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;

public class CallbackMapper {

    public static Callback readCallback(String rawCallback) {
        var callbackArgs = rawCallback.split(CALLBACK_DELIMITER);
        var source = CallbackSource.fromCode(callbackArgs[0]);
        switch (source) {
            case COLLECTIONS -> {
                return CollectionsCallback.builder()
                    .source(source)
                    .action(CollectionCallbackAction.fromCode(callbackArgs[1]))
                    .data(callbackArgs[2])
                    .build();
            }
            case SETTINGS -> {
                return SettingsCallback.builder()
                    .source(source)
                    .action(SettingsCallbackAction.fromCode(callbackArgs[1]))
                    .data(callbackArgs[2])
                    .build();
            }
            case NEW_CARD -> {
                return NewCardCallback.builder()
                    .source(source)
                    .action(NewCardCallbackAction.fromCode(callbackArgs[1]))
                    .data(callbackArgs[2])
                    .build();
            }
            default -> throw new IllegalArgumentException("Unhandled callback action: " + source);
        }
    }

    public static String writeCallback(Callback callback) {
        return callback.getSource().getCode()
            + CALLBACK_DELIMITER
            + callback.getActionCode()
            + CALLBACK_DELIMITER
            + callback.getData();
    }
}
