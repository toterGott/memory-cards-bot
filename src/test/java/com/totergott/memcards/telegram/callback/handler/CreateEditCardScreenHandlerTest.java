package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.TestUtils.DEFAULT_LOCALE;
import static com.totergott.memcards.TestUtils.getCallbackUpdate;
import static com.totergott.memcards.TestUtils.getCommandUpdate;
import static com.totergott.memcards.TestUtils.getMessageUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.card.CardRepository;
import com.totergott.memcards.collection.CardCollectionRepository;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.TelegramUpdateConsumer;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback;
import com.totergott.memcards.telegram.callback.model.CreateEditCardCallback.CreateEditCardCallbackAction;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootTest
class CreateEditCardScreenHandlerTest extends BaseTest {

    @Autowired
    private TextProvider textProvider;
    @Autowired
    private TelegramUpdateConsumer telegramUpdateConsumer;
    @Autowired
    private CardCollectionRepository cardCollectionRepository;
    @Autowired
    private CardRepository cardRepository;

    public static final String NEW_CARD_QUESTION = "New card question";

    @Test
    void whenCardJustCreated_thenCollectionChosen_thenLastChosenIdIsSet() throws TelegramApiException {
        var lastMessage = createCardAndGetLastMessage();
        var changeCollectionQueryData = ((InlineKeyboardMarkup) lastMessage.getReplyMarkup()).getKeyboard()
            .get(1)
            .getFirst().getCallbackData();
        var pushChangeCollection = getCallbackUpdate(changeCollectionQueryData);
        clearInvocations(telegramClient);
        telegramUpdateConsumer.consume(pushChangeCollection);
        var responseCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramClient, times(1)).execute(responseCaptor.capture());
        var buttonWithNonDefaultCollection = responseCaptor.getValue().getReplyMarkup().getKeyboard().stream()
            .flatMap(Collection::stream)
            .filter(it -> it.getText().equals(textProvider.get("tutorial_collection_name")))
            .findFirst().get();

        var selectNonDefaultCollection = getCallbackUpdate(buttonWithNonDefaultCollection.getCallbackData());
        clearInvocations(telegramClient);
        telegramUpdateConsumer.consume(selectNonDefaultCollection);

        var collections = cardCollectionRepository.findAll();
        assertThat(collections).hasSize(2);
        var tutorialCollection = collections.stream().filter(it -> it.getName().equals(textProvider.get(
            "tutorial_collection_name"))).findFirst().get();
        var cards = cardRepository.findAllByCollectionId(tutorialCollection.getId(), PageRequest.of(0, 100))
            .getContent();
        var newCard = cards.getLast();
        assertThat(newCard.getQuestion()).isEqualTo(NEW_CARD_QUESTION);
        var user = userRepository.findAll().getFirst();
        assertThat(user.getPayload().getLastChosenCollectionId()).isEqualTo(tutorialCollection.getId());
    }

    @Test
    void whenCardsLimitReached_thenTryToCreateNewCard_thenCardNotCreated() throws TelegramApiException {
        telegramUpdateConsumer.consume(getCommandUpdate());
        var user = userRepository.findAll().getFirst();
        userRepository.save(user);
        var repeat = 50 - cardRepository.count();
        for (int i = 0; i < repeat; i++) {
            telegramUpdateConsumer.consume(getMessageUpdate(textProvider.get("button.new_card")));
            telegramUpdateConsumer.consume(getMessageUpdate("card " + i));
            telegramUpdateConsumer.consume(getMessageUpdate("answer " + i));
            telegramUpdateConsumer.consume(getCallbackUpdate(CreateEditCardCallback.builder().action(
                CreateEditCardCallbackAction.CONFIRM).build()));
        }
        clearInvocations(telegramClient);
        ArgumentCaptor<SendMessage> sendMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);

        telegramUpdateConsumer.consume(getMessageUpdate(textProvider.get("button.new_card")));

        assertThat(cardRepository.count()).isEqualTo(50);
        verify(telegramClient, times(3)).execute(sendMessageCaptor.capture());
        var lastMessage = sendMessageCaptor.getAllValues().getLast().getText();
        assertThat(lastMessage).isEqualTo(textProvider.get("card.limit_reached"));
    }

    private SendMessage createCardAndGetLastMessage() throws TelegramApiException {
        var createCard = getMessageUpdate(textProvider.getMessage("button.new_card", DEFAULT_LOCALE));
        telegramUpdateConsumer.consume(createCard);
        var inputQuestion = getMessageUpdate(NEW_CARD_QUESTION);
        telegramUpdateConsumer.consume(inputQuestion);
        clearInvocations(telegramClient);
        var inputAnswer = getMessageUpdate("New card answer");
        telegramUpdateConsumer.consume(inputAnswer);
        var responseCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(3)).execute(responseCaptor.capture());
        verify(telegramClient, times(3)).execute(any(DeleteMessage.class));
        return responseCaptor.getAllValues().getLast();
    }
}