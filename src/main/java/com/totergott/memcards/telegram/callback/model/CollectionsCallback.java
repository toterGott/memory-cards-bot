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
public class CollectionsCallback extends Callback {

    @Builder.Default
    protected CallbackSource source = CallbackSource.COLLECTIONS;

    private CollectionCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = CollectionCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(CollectionCallbackAction action) {
        this.action = action;
    }

    @RequiredArgsConstructor
    public enum CollectionCallbackAction implements EncodedEnum {
        SELECT("S"),
        NEW_COLLECTION("N"),
        FOCUS_ON_COLLECTION("F"),
        BACK("B"),
        BROWSE_CARDS("E"),
        DELETE("d"),
        CONFIRM_DELETE("D");

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static CollectionCallbackAction fromCode(String code) {
            return Arrays.stream(CollectionCallbackAction.values()).filter(it -> it.getCode().equals(code)).findFirst()
                .orElse(null);
        }
    }
}
