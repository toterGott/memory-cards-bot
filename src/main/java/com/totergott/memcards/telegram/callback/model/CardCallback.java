package com.totergott.memcards.telegram.callback.model;

import java.util.Arrays;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class CardCallback extends Callback {

    @Builder.Default
    protected CallbackSource source = CallbackSource.CARD;
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
        DELETE_DIALOG("d"),
        CONFIRM_DELETE("D"),
        CHANGE_COLLECTION("C"),
        SET_COLLECTION("S"),
        CANCEL_DELETE("c"),
        EDIT("E"),
        CHANGE_PAGE("P"),
        SELECT("s"),
        BACK("b"),
        SHOW_ANSWER("A"),
        CHECK_KNOWLEDGE("K"),
        CONFIGS("O"),
        CHECK_INFO("i"),
        NEXT_CARD("N"),
        BACK_TO_CARD("B"),
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
