package com.totergott.memcards;

import static com.totergott.memcards.telegram.Constants.START_COMMAND;
import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totergott.memcards.telegram.callback.model.Callback;
import com.totergott.memcards.user.AvailableLocale;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@UtilityClass
public class TestUtils {

    public static final Random RANDOM = new Random();
    public static final AvailableLocale DEFAULT_LOCALE = AvailableLocale.EN;
    public static final String DEFAULT_LANGUAGE_CODE = DEFAULT_LOCALE.getTag();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final int ASCII_PRINTABLE_START = 33;
    private final int ASCII_PRINTABLE_RANGE = 94;

    public static Update getMessageUpdate(String message) {
        var update = getMessageUpdate();
        update.getMessage().setText(message);
        return update;
    }

    public static Update getMessageUpdate() {
        var update = readJsonFileToUpdate("message-update.json");
        update.setUpdateId(RANDOM.nextInt());
        update.getMessage().setMessageId(RANDOM.nextInt());
        return update;
    }

    public static Update getCommandUpdate(String command) {
        var update = getCommandUpdate();

        var commandEntity = MessageEntity.builder()
            .type("bot_command")
            .text(START_COMMAND)
            .offset(0)
            .length(command.length())
            .build();
        update.getMessage().setEntities(List.of(commandEntity));
        update.getMessage().setText(command);

        return update;
    }

    public static Update getCommandUpdate() {
        var update = readJsonFileToUpdate("command-update.json");
        update.setUpdateId(RANDOM.nextInt());
        update.getMessage().setMessageId(RANDOM.nextInt());
        return update;
    }


    public static Update getCallbackUpdate(String callbackData) {
        var update = getCallbackUpdate();
        var callbackQuery = update.getCallbackQuery();
        callbackQuery.setData(callbackData);
        return update;
    }

    public static Update getCallbackUpdate(Callback callback) {
        var update = getCallbackUpdate();
        var callbackQuery = update.getCallbackQuery();
        callbackQuery.setData(writeCallback(callback));
        return update;
    }

    public static Update getCallbackUpdate() {
        var update = readJsonFileToUpdate("callback-update.json");
        var callbackQuery = update.getCallbackQuery();
        callbackQuery.setId(String.valueOf(RANDOM.nextLong()));
        ((Message) callbackQuery.getMessage()).setMessageId(RANDOM.nextInt());
        return update;
    }

    private static Update readJsonFileToUpdate(String fileName) {
        try (var inputStream = TestUtils.class.getClassLoader().getResourceAsStream("json/" + fileName)) {
            if (inputStream == null) {
                throw new IOException("File not found: " + fileName);
            }

            var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            try {
                return objectMapper.readValue(json, Update.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateRandomString(int length) {
        return IntStream.range(0, length)
            .mapToObj(i -> String.valueOf((char) (RANDOM.nextInt(ASCII_PRINTABLE_RANGE) + ASCII_PRINTABLE_START)))
            .collect(Collectors.joining());
    }
}
