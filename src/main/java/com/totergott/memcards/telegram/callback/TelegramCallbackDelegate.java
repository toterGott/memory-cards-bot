package com.totergott.memcards.telegram.callback;

import static com.totergott.memcards.telegram.callback.CallbackMapper.readCallback;

import com.totergott.memcards.telegram.TelegramClientWrapper;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.telegram.callback.model.CallbackSource;
import com.totergott.memcards.user.TelegramUser;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramCallbackDelegate {

    private final TelegramClientWrapper client;
    private final Set<CallbackHandler> callbackHandlers;
    private Map<CallbackSource, Optional<CallbackHandler>> callbackHandlerMap;

    @PostConstruct
    public void init() {
        callbackHandlerMap = callbackHandlers.stream()
            .collect(Collectors.toMap(
                CallbackHandler::getCallbackSource,
                Optional::of
            ));
    }

    public void handleCallback(CallbackQuery callbackQuery, TelegramUser user) {
        Callback callback = readCallback(callbackQuery.getData());
        var handler = callbackHandlerMap.get(callback.getSource()).orElseThrow(
            () -> new IllegalStateException("Unexpected action source value: " + callback.getSource())
        );
        handler.handle(callback, callbackQuery, user);

        var callbackId = callbackQuery.getId();
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        client.execute(answer);
    }
}
