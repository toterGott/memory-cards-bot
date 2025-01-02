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
public class CardCallback extends Callback {

    private CardCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = CardCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(CardCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum CardCallbackAction implements EncodedEnum {
        DELETE("d"),
        DELETE_CONFIRM("D"),
        CHANGE_COLLECTION("C"),
        CANCEL("c")
        ;

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static CardCallbackAction fromCode(String code) {
            return Arrays.stream(CardCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
