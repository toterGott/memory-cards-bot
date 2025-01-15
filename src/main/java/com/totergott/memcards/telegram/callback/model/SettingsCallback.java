package com.totergott.memcards.telegram.callback.model;

import java.util.Arrays;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class SettingsCallback extends Callback {

    @Builder.Default
    private CallbackSource source = CallbackSource.SETTINGS;

    private SettingsCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = SettingsCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(SettingsCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum SettingsCallbackAction implements EncodedEnum {
        LANGUAGE("l"),
        CHANNEL_LANGUAGE("L"),
        INFO("i"),
        ;

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static SettingsCallbackAction fromCode(String code) {
            return Arrays.stream(SettingsCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
