package com.totergott.memcards;

import static com.totergott.memcards.telegram.Constants.START_COMMAND;

import com.totergott.memcards.user.AvailableLocale;
import java.util.List;
import java.util.Random;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@UtilityClass
public class TestUtils {

    public static final Random RANDOM = new Random();
    public static final AvailableLocale DEFAULT_LOCALE = AvailableLocale.EN;
    public static final String DEFAULT_LANGUAGE_CODE = DEFAULT_LOCALE.getTag();


    public static Update getUpdateWithMessage() {
        var update = new Update();
        var message = new Message();
        message.setMessageId(RANDOM.nextInt());
        var chatId = RANDOM.nextLong();

        var chat = Chat.builder().id(chatId).type("private").build();
        message.setChat(chat);

        var from =
            User.builder().id(chatId).languageCode(DEFAULT_LANGUAGE_CODE).firstName("FirstName").isBot(false).userName("testUser").build();
        message.setFrom(from);

        update.setMessage(message);
        return update;
    }

    public static Update getUpdateWithCommand(String command) {
        var update = getUpdateWithMessage();
        var commandEntity = MessageEntity.builder().type("bot_command").text(START_COMMAND).offset(0)
            .length(command.length()).build();
        update.getMessage().setEntities(List.of(commandEntity));
        update.getMessage().setText(command);
        return update;
    }
}
