package com.totergott.memcards.telegram;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@TestConfiguration
@ActiveProfiles("test")
public class TelegramBotConfigTest {

    @Bean
    @Primary
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication() {
        return mock(TelegramBotsLongPollingApplication.class);
    }

    @Bean
    @Primary
    public TelegramClient telegramClient() {
        return mock(TelegramClient.class);
    }
}
