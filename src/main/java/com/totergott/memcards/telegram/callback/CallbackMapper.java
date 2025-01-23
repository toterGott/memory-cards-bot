package com.totergott.memcards.telegram.callback;

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

    private static final String CALLBACK_DELIMITER = " ";
    private static final String GRADE_KEY = "!";
    private static final String PAGE_KEY = "#";
    private static final String BREADCRUMB_KEY = "/";

    public static Callback readCallback(String rawCallback) {
        var callbackArgs = rawCallback.split(CALLBACK_DELIMITER);
        var callbackSource = CallbackSource.fromCode(callbackArgs[0]);
        var actionCode = callbackArgs[1];
        var data = callbackArgs[2];
        var pageNumber = getIntValueByKey(callbackArgs, PAGE_KEY);
        var breadCrumb = CallbackSource.fromCode(getStringValueByKey(callbackArgs, BREADCRUMB_KEY));

        var callback = switch (callbackSource) {
            case COLLECTIONS -> CollectionsCallback.builder()
                .action(CollectionCallbackAction.fromCode(actionCode))
                .data(data)
                .pageNumber(pageNumber)
                .breadCrumb(breadCrumb)
                .build();
            case SETTINGS -> SettingsCallback.builder()
                .action(SettingsCallbackAction.fromCode(actionCode))
                .data(data)
                .breadCrumb(breadCrumb)
                .build();
            case NEW_CARD -> CreateEditCardCallback.builder()
                .action(CreateEditCardCallbackAction.fromCode(actionCode))
                .data(data)
                .breadCrumb(breadCrumb)
                .build();
            case GET_CARD -> GetCardCallback.builder()
                .action(GetCardCallbackAction.fromCode(actionCode))
                .grade(getIntValueByKey(callbackArgs, GRADE_KEY))
                .data(data)
                .breadCrumb(breadCrumb)
                .build();
            case SCHEDULE -> ScheduleCallback.builder()
                .source(callbackSource)
                .action(ScheduleCallbackAction.fromCode(actionCode))
                .build();
            case PAGE_NAVIGATION -> PageNavigationCallback.builder()
                .action(PageNavigationCallbackAction.fromCode(actionCode))
                .data(data)
                .breadCrumb(breadCrumb)
                .pageNumber(pageNumber)
                .build();
            default -> throw new IllegalArgumentException("Unhandled callback action: " + callbackSource);
        };
        log.debug("Callback: {}", callback);
        return callback;
    }

    private static Integer getIntValueByKey(String[] callbackArgs, String key) {
        var res = getStringValueByKey(callbackArgs, key);
        if (res == null || res.equals("null")) {
            return null;
        }
        return Integer.parseInt(res);
    }

    private static String getStringValueByKey(String[] callbackArgs, String key) {
        for (int i = 3; i < callbackArgs.length; i++) {
            if (callbackArgs[i].startsWith(key)) {
                return callbackArgs[i].substring(key.length());
            }
        }
        return null;
    }

    public static String writeCallback(Callback callback) {
        StringBuilder builder = new StringBuilder();

        builder.append(callback.getSource().getCode());
        builder.append(CALLBACK_DELIMITER);
        builder.append(callback.getActionCode());
        builder.append(CALLBACK_DELIMITER);
        builder.append(callback.getData());

        if (callback.getBreadCrumb() != null) {
            builder.append(CALLBACK_DELIMITER);
            builder.append(BREADCRUMB_KEY);
            builder.append(callback.getBreadCrumb().getCode());
        }

        if (callback.getPageNumber() != null) {
            builder.append(CALLBACK_DELIMITER);
            builder.append(PAGE_KEY);
            builder.append(callback.getPageNumber());
        }

        if (callback instanceof GetCardCallback) {
            builder.append(CALLBACK_DELIMITER);
            builder.append(GRADE_KEY);
            builder.append(((GetCardCallback) (callback)).getGrade());
        }

        return builder.toString();
    }
}
