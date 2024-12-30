package com.example.memcards.i18n;

import com.example.memcards.user.AvailableLocale;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageProvider {

    private final MessageSource messageSource;

    private static final Map<AvailableLocale, Map<String, String>> localeToMessagesByCode = new HashMap<>();

    @PostConstruct
    public void init() {
        Arrays.stream(AvailableLocale.values()).forEach(locale -> {
            localeToMessagesByCode.put(locale, loadMessagesToMap(locale));
        });
    }

    private Map<String, String> loadMessagesToMap(AvailableLocale availableLocale) {
        Map<String, String> messagesMap = new HashMap<>();
        String basename = "messages";
        ResourceBundle bundle = ResourceBundle.getBundle(basename, availableLocale.getLocale());
        bundle.keySet().forEach(key -> messagesMap.put(bundle.getString(key), key));
        return messagesMap;
    }

    public String getMessage(String code, AvailableLocale availableLocale) {
        return messageSource.getMessage(code, null, availableLocale.getLocale());
    }

    public String getMessage(String code, AvailableLocale availableLocale, String... args) {
        return messageSource.getMessage(code, args, availableLocale.getLocale());
    }

    public String resolveCode(String text, AvailableLocale languageCode) {
        var langMap = localeToMessagesByCode.get(languageCode);
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
        for(Map<String, String> messageByKey : localeToMessagesByCode.values()) {
            String code = messageByKey.get(text);
            if (code != null) {
                return code;
            }
        }
        return null;
    }
}
