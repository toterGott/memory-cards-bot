package com.totergott.memcards.telegram;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.meta.api.objects.Update;

@TestConstructor(autowireMode = AutowireMode.ALL)
class TelegramUpdateConsumerTest extends BaseTest {

    private final TelegramUpdateConsumer telegramUpdateConsumer;

    @MockitoBean
    private final TelegramUpdateHandler telegramUpdateHandler;

    public TelegramUpdateConsumerTest(
        TelegramUpdateConsumer telegramUpdateConsumer,
        TelegramUpdateHandler telegramUpdateHandler,
        UserRepository userRepository
    ) {
        this.telegramUpdateConsumer = telegramUpdateConsumer;
        this.telegramUpdateHandler = telegramUpdateHandler;
        this.userRepository = userRepository;
    }

    @Test
    public void receiveUpdate() {
        var update = new Update();

        telegramUpdateConsumer.consume(update);

        verify(telegramUpdateHandler, times(1)).handleUpdate(eq(update));
    }
}
