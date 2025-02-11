package com.totergott.memcards.telegram;

import static com.totergott.memcards.TestUtils.DEFAULT_LANGUAGE_CODE;
import static com.totergott.memcards.TestUtils.DEFAULT_LOCALE;
import static com.totergott.memcards.TestUtils.RANDOM;
import static com.totergott.memcards.TestUtils.getUpdateWithCommand;
import static com.totergott.memcards.telegram.Constants.MENU_COMMAND;
import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static com.totergott.memcards.user.UserState.STAND_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.user.UserRepository;
import com.totergott.memcards.user.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TelegramUpdateConsumer telegramUpdateConsumer;
    @Autowired
    private TelegramClient telegramClient;
    @Autowired
    private TextProvider textProvider;
    @Autowired
    private UserService userService;

    public CommandHandlerTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Test
    void whenNoUsers_firstStartCommand_thenUserCreated() throws TelegramApiException {
        var update = getUpdateWithCommand(START_COMMAND);

        telegramUpdateConsumer.consume(update);

        var sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(1)).execute(sendCaptor.capture());
        var welcomeText = textProvider.getMessage("welcome", DEFAULT_LOCALE);
        assertThat(sendCaptor.getValue().getText()).isEqualTo(welcomeText);
        var deleteCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramClient, times(1)).execute(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().getMessageId()).isEqualTo(update.getMessage().getMessageId());
        assertThat(deleteCaptor.getValue().getChatId()).isEqualTo(update.getMessage().getChatId().toString());

        var users = userRepository.findAll();
        assertThat(users).hasSize(1);
        var user = users.getFirst();
        assertThat(user.getState()).isEqualTo(STAND_BY);
        // todo check default collections created
    }

    @Test
    void whenUserExists_hasMessageAndMenuCommand_thenMenuHandled() throws TelegramApiException {
        var update = getUpdateWithCommand(MENU_COMMAND);
        var user = userService.createUser(update.getMessage().getChat(), DEFAULT_LANGUAGE_CODE);
        user.getPayload().setChatMessages(List.of(RANDOM.nextInt()));
        user = userService.save(user);

        telegramUpdateConsumer.consume(update);

        var deleteCaptor = ArgumentCaptor.forClass(DeleteMessage.class);
        verify(telegramClient, times(2)).execute(deleteCaptor.capture());
        assertThat(deleteCaptor.getAllValues().getFirst().getMessageId()).isEqualTo(user.getPayload().getChatMessages().getFirst());
        assertThat(deleteCaptor.getAllValues().getFirst().getChatId()).isEqualTo(user.getChatId().toString());
        assertThat(deleteCaptor.getAllValues().getLast().getMessageId()).isEqualTo(update.getMessage().getMessageId());
        assertThat(deleteCaptor.getAllValues().getLast().getChatId()).isEqualTo(update.getMessage().getChatId().toString());

        var sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(1)).execute(sendCaptor.capture());
        var welcomeText = textProvider.getMessage("main_menu", DEFAULT_LOCALE);
        assertThat(sendCaptor.getValue().getText()).isEqualTo(welcomeText);
    }

}