package com.example.memcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
@EnableJpaAuditing
public class MemoryCardsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoryCardsApplication.class, args);
	}

}
