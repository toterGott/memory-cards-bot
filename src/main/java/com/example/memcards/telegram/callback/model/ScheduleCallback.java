package com.example.memcards.telegram.callback.model;

import java.util.Arrays;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ScheduleCallback extends Callback {

    private ScheduleCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = ScheduleCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(ScheduleCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum ScheduleCallbackAction implements EncodedEnum {
        DISABLE("D"),
        SET_TIME("T")
        ;

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static ScheduleCallbackAction fromCode(String code) {
            return Arrays.stream(ScheduleCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
