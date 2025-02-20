package com.totergott.memcards.telegram;

import static com.totergott.memcards.TestUtils.DEFAULT_LANGUAGE_CODE;
import static com.totergott.memcards.TestUtils.DEFAULT_LOCALE;
import static com.totergott.memcards.TestUtils.RANDOM;
import static com.totergott.memcards.TestUtils.getMessageUpdate;
import static com.totergott.memcards.telegram.callback.CallbackMapper.readCallback;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.callback.model.ScheduleCallback;
import com.totergott.memcards.user.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@TestConstructor(autowireMode = AutowireMode.ALL)
class ReplyKeyboardButtonHandlerTest extends BaseTest {

    @Autowired
    private UserService userService;
    @Autowired
    private TelegramUpdateConsumer telegramUpdateConsumer;
    @Autowired
    private TextProvider textProvider;

    @Test
    void whenUserExists_thenGetScheduleMenu_thenMenuShowed() throws TelegramApiException {
        var update = getMessageUpdate(textProvider.getMessage("button.schedule", DEFAULT_LOCALE));
        var user = userService.createUser(update.getMessage().getChat(), DEFAULT_LANGUAGE_CODE);
        user.getPayload().setChatMessages(List.of(RANDOM.nextInt()));
        userService.save(user);

        telegramUpdateConsumer.consume(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(2)).execute(captor.capture());
        var scheduleConfigMessage = captor.getAllValues().getLast();
        assertThat(scheduleConfigMessage.getText()).isEqualTo(textProvider.getMessage("schedule", DEFAULT_LOCALE));
        var button = ((InlineKeyboardMarkup) scheduleConfigMessage.getReplyMarkup()).getKeyboard().getFirst().getFirst();
        assertThat(button.getText()).isEqualTo("10 " + MINUTES.name().toLowerCase(), DEFAULT_LOCALE);
        ScheduleCallback callback = (ScheduleCallback) readCallback(button.getCallbackData());
        assertThat(callback.getData()).isEqualTo("0");
    }
}