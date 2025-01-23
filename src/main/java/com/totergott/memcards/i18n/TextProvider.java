package com.totergott.memcards.i18n;

import static com.totergott.memcards.telegram.TelegramUtils.getLanguage;
import static com.totergott.memcards.telegram.TelegramUtils.getLocale;

import com.totergott.memcards.user.AvailableLocale;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextProvider {

    private final MessageSource messageSource;

    private static final Map<AvailableLocale, Map<String, String>> localeToMessagesByCode = new HashMap<>();

    @PostConstruct
    public void init() {
        Arrays.stream(AvailableLocale.values())
            .forEach(locale -> localeToMessagesByCode.put(locale, loadMessagesToMap(locale)));
    }

    private Map<String, String> loadMessagesToMap(AvailableLocale availableLocale) {
        Map<String, String> messagesMap = new HashMap<>();
        String basename = "messages";
        ResourceBundle bundle = ResourceBundle.getBundle(basename, availableLocale.getLocale());
        bundle.keySet().forEach(key -> messagesMap.put(bundle.getString(key), key));
        return messagesMap;
    }

    public String appendPageInfo(String text, Page<?> page) {
        return text + "\n" + get(
            "page.info",
            String.valueOf(page.getNumber() + 1),
            String.valueOf(page.getTotalPages()),
            String.valueOf(page.getTotalElements())
        );
    }

    public String getMessage(String code, AvailableLocale availableLocale) {
        return messageSource.getMessage(code, null, availableLocale.getLocale());
    }

    public String getMessage(String code, AvailableLocale availableLocale, String... args) {
        return messageSource.getMessage(code, args, availableLocale.getLocale());
    }

    public String get(String code, String... args) {
        return messageSource.getMessage(code, args, getLanguage().getLocale());
    }

    public String get(String code) {
        return messageSource.getMessage(code, null, getLocale());
    }

    public String resolveCode(String text) {
        var langMap = localeToMessagesByCode.get(getLanguage());
        if (langMap == null) {
            langMap = localeToMessagesByCode.get(AvailableLocale.EN);
        }
        var code = langMap.get(text);
        if (code == null) {
            code = findInAllLocales(text);
        }
        return code;
    }

    private String findInAllLocales(String text) {
        for (Map<String, String> messageByKey : localeToMessagesByCode.values()) {
            String code = messageByKey.get(text);
            if (code != null) {
                return code;
            }
        }
        return null;
    }
}
