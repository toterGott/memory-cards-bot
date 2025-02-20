package com.totergott.memcards.telegram.callback.handler;

import static com.totergott.memcards.TestUtils.getCallbackUpdate;
import static com.totergott.memcards.TestUtils.getCommandUpdate;
import static com.totergott.memcards.telegram.callback.CallbackMapper.writeCallback;
import static org.assertj.core.api.Assertions.assertThat;

import com.totergott.memcards.BaseTest;
import com.totergott.memcards.collection.CardCollection;
import com.totergott.memcards.collection.CardCollectionRepository;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.telegram.TelegramUpdateConsumer;
import com.totergott.memcards.telegram.callback.model.SettingsCallback;
import com.totergott.memcards.telegram.callback.model.SettingsCallback.SettingsCallbackAction;
import com.totergott.memcards.user.AvailableLocale;
import com.totergott.memcards.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Test
    void whenUserExists_thenCallbackToChangeMessageSent_thenLanguageIsChanged() {
        telegramUpdateConsumer.consume(getCommandUpdate());
        SettingsCallback callback = SettingsCallback.builder()
            .action(SettingsCallbackAction.CHANGE_LANGUAGE)
            .data(AvailableLocale.RU.name())
            .build();
        var update = getCallbackUpdate(writeCallback(callback));

        telegramUpdateConsumer.consume(update);

        var user = userRepository.findAll().getFirst();
        assertThat(user.getLanguage()).isEqualTo(AvailableLocale.RU);
        var defaultCollectionName = textProvider.getMessage("default_collection_name", AvailableLocale.RU);
        var tutorialCollectionName = textProvider.getMessage("tutorial_collection_name", AvailableLocale.RU);
        var collectionNames = cardCollectionRepository.findAll().stream().map(CardCollection::getName).toList();
        assertThat(collectionNames).hasSize(2);
        assertThat(collectionNames).contains(defaultCollectionName);
        assertThat(collectionNames).contains(tutorialCollectionName);
    }
}