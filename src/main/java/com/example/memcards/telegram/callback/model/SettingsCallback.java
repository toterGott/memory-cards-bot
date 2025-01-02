package com.example.memcards.telegram.callback.model;

import com.example.memcards.telegram.callback.model.CollectionsCallback.CollectionCallbackAction;
import java.util.Arrays;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class SettingsCallback extends Callback {

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
