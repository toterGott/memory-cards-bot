package com.totergott.memcards;

import java.util.Random;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;
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
        update.setMessage(message);
        return update;
    }
}
