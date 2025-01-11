package com.totergott.memcards.user;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AvailableLocale {
    EN("en", Locale.of("en"), "English"),
    RU("ru", Locale.of("ru"), "Русский"),
    ;

    private final String tag;
    private final Locale locale;
    private final String readableName;
}
