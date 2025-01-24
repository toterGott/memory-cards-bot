package com.totergott.memcards.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@Profile("!test")
public class TelegramBotConfig {

    @Value("${bot.token}")
    private String botToken;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication(TelegramUpdateConsumer telegramUpdateConsumer) {
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, telegramUpdateConsumer);
            return botsApplication;
        } catch (TelegramApiException e) {
            throw new Error("Fatal error on bot initialization", e);
        }
    }

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient(botToken);
    }
}
