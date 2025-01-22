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
public class GetCardCallback extends Callback {

    @Builder.Default
    protected CallbackSource source = CallbackSource.GET_CARD;
    private GetCardCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = GetCardCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(GetCardCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum GetCardCallbackAction implements EncodedEnum {
        CHOOSE_ANOTHER_COLLECTION("C"),
        SET_COLLECTION("S"),
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
        public static GetCardCallbackAction fromCode(String code) {
            return Arrays.stream(GetCardCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
