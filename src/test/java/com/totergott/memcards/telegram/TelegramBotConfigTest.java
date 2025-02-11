package com.totergott.memcards.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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
    public TelegramClient telegramClient() throws TelegramApiException {
        var telegramClient = mock(TelegramClient.class);
        when(telegramClient.execute(any(SendMessage.class))).thenAnswer(_ -> Message.builder().messageId(new Random().nextInt()).build());
        return telegramClient;
    }
}
