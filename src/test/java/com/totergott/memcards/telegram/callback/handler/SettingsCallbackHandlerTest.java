package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.TestUtils.getCallbackUpdate;
import static com.totergott.memcards.TestUtils.getCommandUpdate;
import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;
import static org.assertj.core.api.Assertions.assertThat;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardRepository;
import com.totergott.memcards.collection.CardCollection;
import com.totergott.memcards.collection.CardCollectionRepository;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.TelegramUpdateConsumer;
import com.totergott.memcards.telegram.callback.model.SettingsCallback;
import com.totergott.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;
import com.totergott.memcards.user.AvailableLocale;
import com.totergott.memcards.user.UserRepository;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class SettingsCallbackHandlerTest extends BaseTest {

    @Autowired
    private TelegramUpdateConsumer telegramUpdateConsumer;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TextProvider textProvider;
    @Autowired
    private CardCollectionRepository cardCollectionRepository;
    @Autowired
    private CardRepository cardRepository;

    public static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(
                "en",
                AvailableLocale.RU,
                "На этом все?"
            ),
            Arguments.of(
                "ru",
                AvailableLocale.EN,
                "Is that all?"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void whenUserExists_thenCallbackToChangeMessageSent_thenLanguageIsChanged(
        String initialLanguage, AvailableLocale targetLanguage, String expectedMessage
    ) {
        var startCommand = getCommandUpdate();
        startCommand.getMessage().getFrom().setLanguageCode(initialLanguage);
        telegramUpdateConsumer.consume(startCommand);
        SettingsCallback callback = SettingsCallback.builder()
            .action(SettingsCallbackAction.CHANGE_LANGUAGE)
            .data(targetLanguage.name())
            .build();
        var update = getCallbackUpdate(writeCallback(callback));

        telegramUpdateConsumer.consume(update);

        var user = userRepository.findAll().getFirst();
        assertThat(user.getLanguage()).isEqualTo(targetLanguage);
        var defaultCollectionName = textProvider.getMessage("default_collection_name", targetLanguage);
        var tutorialCollectionName = textProvider.getMessage("tutorial_collection_name", targetLanguage);
        var collections = cardCollectionRepository.findAll();
        assertThat(collections).hasSize(2);
        var collectionNames = collections.stream().map(CardCollection::getName).toList();
        assertThat(collectionNames).contains(defaultCollectionName);
        var tutorialCollection = collections.stream().filter(collection -> collection.getName().equals(tutorialCollectionName)).findFirst().get();
        assertThat(tutorialCollection.getName()).isEqualTo(tutorialCollectionName);
        var cards = cardRepository.findAllByCollectionId(tutorialCollection.getId(), PageRequest.of(0, 100)).getContent();
        var cardQuestions = cards.stream().map(Card::getQuestion).toList();
        assertThat(cardQuestions).hasSize(6);
        assertThat(cardQuestions).contains(expectedMessage);
    }
}