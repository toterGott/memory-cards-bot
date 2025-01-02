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
public class NewCardCallback extends Callback {

    private NewCardCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = NewCardCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(NewCardCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum NewCardCallbackAction implements EncodedEnum {
        CONFIRM("c"),
        CHANGE_COLLECTION("C"),
        SET_COLLECTION("S"),
        CHANGE_PAGE("P");

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static NewCardCallbackAction fromCode(String code) {
            return Arrays.stream(NewCardCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
