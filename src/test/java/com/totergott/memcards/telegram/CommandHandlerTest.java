package com.totergott.memcards.telegram;

import static com.totergott.memcards.TestUtils.getUpdateWithCommand;
import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@SpringBootTest
@TestConstructor(autowireMode = AutowireMode.ALL)
class CommandHandlerTest extends BaseTest {

    private final TelegramUpdateConsumer telegramUpdateConsumer;
    private final TelegramClient telegramClient;
    private final MessageService messageService;

    public CommandHandlerTest(
        TelegramUpdateConsumer telegramUpdateConsumer,
        UserRepository userRepository,
        TelegramClient telegramClient,
        MessageService messageService
    ) {
        this.telegramUpdateConsumer = telegramUpdateConsumer;
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.telegramClient = telegramClient;
    }

    @Test
    void whenNoUsers_firstStartCommand_thenUserCreated() throws TelegramApiException {
        var update = getUpdateWithCommand(START_COMMAND);

        telegramUpdateConsumer.consume(update);

        verify(telegramClient, times(1)).execute(any(SendMessage.class));
        verify(telegramClient, times(1)).execute(any(DeleteMessage.class));

        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
        var user = users.getFirst();
        assertThat(user.getState()).isEqualTo(STAND_BY);
    }

}