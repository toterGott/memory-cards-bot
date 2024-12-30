package com.example.memcards.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

// todo it's better to create own thread pool
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramUpdateHandler messageHandler;

    @Override
    public void consume(Update update) {
        messageHandler.handleUpdate(update);
        log.debug("Received update: {}", update);
    }
}
