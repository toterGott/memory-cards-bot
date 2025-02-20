package com.totergott.memcards;

import static org.mockito.Mockito.clearInvocations;

import com.totergott.memcards.EnablePostgresTestContainerContextCustomizerFactory.EnabledPostgresTestContainer;
import com.totergott.memcards.telegram.TelegramBotConfigTest;
import com.totergott.memcards.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@Import(TelegramBotConfigTest.class)
@SpringBootTest
@EnabledPostgresTestContainer
public abstract class BaseTest {

    @Autowired
    protected TelegramClient telegramClient;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void init() {
        userRepository.deleteAll();
        clearInvocations(telegramClient);
    }
}
