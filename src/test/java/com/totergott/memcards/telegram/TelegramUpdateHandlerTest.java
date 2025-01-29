package com.totergott.memcards.telegram;

import static com.totergott.memcards.TestUtils.getUpdateWithMessage;
import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@SpringBootTest
@TestConstructor(autowireMode = AutowireMode.ALL)
class TelegramUpdateHandlerTest extends BaseTest {

    private final TelegramUpdateConsumer telegramUpdateConsumer;
    private final TelegramClient telegramClient;

    public TelegramUpdateHandlerTest(
        TelegramUpdateConsumer telegramUpdateConsumer,
        UserRepository userRepository,
        TelegramClient telegramClient
    ) {
        this.telegramUpdateConsumer = telegramUpdateConsumer;
        this.userRepository = userRepository;
        this.telegramClient = telegramClient;
    }

    @Test
    void handleFirstStartCommand() throws TelegramApiException {
        var update = getUpdateWithMessage();
        var commandEntity = MessageEntity.builder().type("bot_command").text(START_COMMAND).offset(0)
            .length(START_COMMAND.length()).build();
        update.getMessage().setEntities(List.of(commandEntity));
        update.getMessage().setText(START_COMMAND);

        telegramUpdateConsumer.consume(update);

        verify(telegramClient, times(1)).execute(any(DeleteMessage.class));
        verify(telegramClient, times(1)).execute(any(SendChatAction.class));
        verify(telegramClient, times(1)).execute(any(SendChatAction.class));

        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
        var user = users.getFirst();
        assertThat(user.getState()).isEqualTo(STAND_BY);
    }
}