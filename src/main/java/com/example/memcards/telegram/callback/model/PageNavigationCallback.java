package com.example.memcards.telegram.callback.model;

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
public class PageNavigationCallback extends Callback {

    protected CallbackSource source = CallbackSource.PAGE_NAVIGATION;

    private PageNavigationCallbackAction action;

    @Override
    public void setAction(String actionName) {
        this.action = PageNavigationCallbackAction.valueOf(actionName);
    }

    @Override
    public String getActionCode() {
        return action.getCode();
    }

    public void setAction(PageNavigationCallbackAction action) {
        this.action = action;
    }

    // tdoo remove
    @RequiredArgsConstructor
    public enum PageNavigationCallbackAction implements EncodedEnum {
        NEXT("N"),
        PREVIOUS("P");

        @Getter
        private final String code;

        // todo could be type parameterized util method
        public static PageNavigationCallbackAction fromCode(String code) {
            return Arrays.stream(PageNavigationCallbackAction.values()).filter(it -> it.getCode().equals(code))
                .findFirst()
                .orElse(null);
        }
    }
}
