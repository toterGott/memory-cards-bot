package com.totergott.memcards.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

//@TestConfiguration
class TelegramBotConfigTest {


    @Value("${bot.token}")
    private String botToken;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication(TelegramUpdateConsumer telegramUpdateConsumer) {
        return null;
    }

    @Bean
    public TelegramClient telegramClient() {
        return null;
    }
}