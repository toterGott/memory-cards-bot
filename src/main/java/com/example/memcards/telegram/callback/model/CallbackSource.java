package com.example.memcards.telegram.callback.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum CallbackSource implements EncodedEnum {
    COLLECTIONS("C"),
    NEW_CARD("N"),
    CARD("c"),
    SETTINGS("S"),
    SCHEDULE("s"),
    ;

    @Getter
    private final String code;

    public static CallbackSource fromCode(String code) {
        return Arrays.stream(CallbackSource.values()).filter(it -> it.getCode().equals(code)).findFirst().orElse(null);
    }
}
