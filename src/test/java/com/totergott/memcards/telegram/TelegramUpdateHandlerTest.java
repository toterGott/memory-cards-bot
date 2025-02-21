package com.totergott.memcards.telegram;

import static com.totergott.memcards.TestUtils.getCommandUpdate;
import static com.totergott.memcards.TestUtils.getMessageUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.collection.CardCollectionRepository;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.user.UserState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootTest
class TelegramUpdateHandlerTest extends BaseTest {

    @Autowired
    private TelegramUpdateConsumer telegramUpdateConsumer;
    public static final String NEW_COLLECTION_NAME = "New Collection";
    @Autowired
    private TextProvider textProvider;
    @Autowired
    private CardCollectionRepository cardCollectionRepository;

    @Test
    void whenUserCreatesCollection_thenSendsCollectionName_thenNewCollectionIsCreated() throws TelegramApiException {
        telegramUpdateConsumer.consume(getCommandUpdate());
        var user = userRepository.findAll().getFirst();
        user.setState(UserState.COLLECTION_CREATION);
        userRepository.save(user);
        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);

        telegramUpdateConsumer.consume(getMessageUpdate(NEW_COLLECTION_NAME));

        verify(telegramClient, times(4)).execute(sendCaptor.capture());
        var lastMessage = sendCaptor.getAllValues().getLast().getText();
        assertThat(lastMessage).contains(NEW_COLLECTION_NAME);
        assertThat(cardCollectionRepository.count()).isEqualTo(3);
        assertThat(cardCollectionRepository.findAll().stream()
                       .filter(it -> it.getName().equals(NEW_COLLECTION_NAME)).findFirst()).isPresent();
    }

    @Test
    void whenUserCreatesCollection_thenReachesLimit_thenNewCollectionIsNotCreated() throws TelegramApiException {
        telegramUpdateConsumer.consume(getCommandUpdate());
        var user = userRepository.findAll().getFirst();
        user.setState(UserState.COLLECTION_CREATION);
        userRepository.save(user);
        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        for (int i = 0; i < 8; i++) {
            telegramUpdateConsumer.consume(getMessageUpdate(NEW_COLLECTION_NAME + i));
            user.setState(UserState.COLLECTION_CREATION);
            userRepository.save(user);
        }
        clearInvocations(telegramClient);

        telegramUpdateConsumer.consume(getMessageUpdate(NEW_COLLECTION_NAME));

        verify(telegramClient, times(3)).execute(sendCaptor.capture());
        var lastMessage = sendCaptor.getAllValues().getLast().getText();
        assertThat(lastMessage).isEqualTo(textProvider.get("collection.limit_reached"));
        assertThat(cardCollectionRepository.count()).isEqualTo(10);
    }

    @Test
    void whenUserCreatesCollection_thenSendExistedName_thenNewCollectionIsNotCreated() throws TelegramApiException {
        telegramUpdateConsumer.consume(getCommandUpdate());
        var user = userRepository.findAll().getFirst();
        user.setState(UserState.COLLECTION_CREATION);
        userRepository.save(user);
        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        telegramUpdateConsumer.consume(getMessageUpdate(NEW_COLLECTION_NAME));
        user.setState(UserState.COLLECTION_CREATION);
        userRepository.save(user);
        clearInvocations(telegramClient);

        telegramUpdateConsumer.consume(getMessageUpdate(NEW_COLLECTION_NAME));

        verify(telegramClient, atLeastOnce()).execute(sendCaptor.capture());
        var lastMessage = sendCaptor.getAllValues().getLast().getText();
        assertThat(lastMessage).isEqualTo(textProvider.get("collection.limit_reached"));
        assertThat(cardCollectionRepository.count()).isEqualTo(3);
    }
}