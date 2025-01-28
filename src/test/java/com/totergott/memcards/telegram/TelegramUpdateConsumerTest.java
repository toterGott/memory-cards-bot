package com.totergott.memcards.telegram;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.objects.Update;

@SpringBootTest
@TestConstructor(autowireMode = AutowireMode.ALL)
@AllArgsConstructor
class TelegramUpdateConsumerTest extends BaseTest {

    private TelegramUpdateConsumer telegramUpdateConsumer;

    @MockitoSpyBean
    private TelegramUpdateHandler messageHandler;

    @Test
    public void when() {
        var update = new Update();

        telegramUpdateConsumer.consume(update);

        verify(messageHandler, times(1)).handleUpdate(eq(update));
    }
}
