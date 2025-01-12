package com.totergott.memcards.collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.i18n.MessageProvider;
import com.totergott.memcards.user.TelegramUser;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CardCollectionRepository repository;
    private final MessageProvider messageProvider;
    private final ObjectMapper objectMapper;
    private final CardService cardService;

    public void initDefaultCollection(TelegramUser user) {
        var collections = repository.findAllByOwnerId(user.getId());
        if (collections.isEmpty()) {
            var defaultCollection = new CardCollection();
            defaultCollection.setName(messageProvider.getMessage("default_collection_name", user.getLanguage()));
            defaultCollection.setOwner(user);
            defaultCollection = repository.save(defaultCollection);
            user.getPayload().setDefaultCollection(defaultCollection.getId());
        }
    }

    public Page<CardCollection> getCollectionsPage(UUID id, int page) {
        var pageRequest = PageRequest.of(page, 3, Sort.by(Order.by("name")));
        return repository.findAllByOwnerId(id, pageRequest);
    }

    public Optional<CardCollection> findById(UUID id) {
        return repository.findById(id);
    }

    public Optional<CardCollection> getById(UUID defaultCollection) {
        return repository.findById(defaultCollection);
    }

    public void deleteById(UUID collectionId) {
//        repository.deleteById(collectionId); // todo find out why is this not working
        repository.deleteByIdQuery(collectionId);
    }

    public CardCollection save(CardCollection collection) {
        return repository.save(collection);
    }

    public void initHowToUserCollection(TelegramUser user) {
        try (InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("default-collections/how-to.en.json")) {
            var map = objectMapper.readValue(
                inputStream, new TypeReference<Map<String, String>>() {
                }
            );
            log.info("How to collection: {}", map);

            var howToCollection = new CardCollection();
            howToCollection.setName(messageProvider.getMessage("how_to_collection_name", user.getLanguage()));
            howToCollection.setOwner(user);
            howToCollection = repository.save(howToCollection);

            var cards = new ArrayList<Card>();
            int appearDelay = 0;
            for (Entry<String, String> entry : map.entrySet()) {
                String question = entry.getKey();
                String answer = entry.getValue();
                var card = new Card();
                card.setQuestion(question);
                card.setAnswer(answer);
                card.setOwner(user);
                card.setCollection(howToCollection);
                card.setAppearTime(Instant.now().plus(appearDelay++, ChronoUnit.MILLIS));
                cards.add(card);
            }
            cardService.saveAll(cards);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
