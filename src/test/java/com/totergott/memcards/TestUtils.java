package com.totergott.memcards;

import java.util.Random;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@UtilityClass
public class TestUtils {

    public static Random random = new Random();

    public static Update getUpdate() {
        var update = new Update();
        return update;
    }

    public static Update getUpdateWithMessage() {
        var update = getUpdate();
        var message = new Message();
        message.setMessageId(random.nextInt());
        var chatId = random.nextLong();

        var chat = Chat.builder().id(chatId).type("private").build();
        message.setChat(chat);

        var from =
            User.builder().id(chatId).languageCode("ru").firstName("FirstName").isBot(false).userName("testUser").build();
        message.setFrom(from);

        update.setMessage(message);
        return update;
    }
}
