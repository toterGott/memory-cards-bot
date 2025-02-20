package com.totergott.memcards.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    public void consume(Update update) {
        try {
            var strUpdate = objectMapper.writeValueAsString(update);
            log.info("Message received: {}", strUpdate);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        messageHandler.handleUpdate(update);
        log.debug("Received update: {}", update);
    }
}
