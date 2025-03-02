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
public class CreateEditCardCallback extends Callback {

    private CreateEditCardCallbackAction action;

    @Builder.Default
    protected CallbackSource source = CallbackSource.NEW_CARD;

    @Override
    public void setAction(String actionName) {
        this.action = CreateEditCardCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(CreateEditCardCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum CreateEditCardCallbackAction implements EncodedEnum {
        CONFIRM("c"),
        EDIT_COLLECTION("C"),
        SET_COLLECTION("S"),
        CHANGE_PAGE("P"),
        EDIT_QUESTION("E"),
        EDIT_ANSWER("e"),
        DELETE_DIALOG("d"),
        CONFIRM_DELETE("D"),
        CANCEL_DELETE("b"),
        ;

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static CreateEditCardCallbackAction fromCode(String code) {
            return Arrays.stream(CreateEditCardCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
