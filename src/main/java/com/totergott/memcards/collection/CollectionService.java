package com.totergott.memcards.collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totergott.memcards.card.Card;
import com.totergott.memcards.card.CardService;
import com.totergott.memcards.i18n.TextProvider;
import com.totergott.memcards.user.AvailableLocale;
import com.totergott.memcards.user.TelegramUser;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    @Value("${app.page_size}")
    private Integer pageSize;

    @Value("${app.limit.collections}")
    private Integer collectionsLimit;

    private final CardCollectionRepository repository;
    private final TextProvider textProvider;
    private final ObjectMapper objectMapper;
    private final CardService cardService;

    public void initDefaultCollection(TelegramUser user) {
        var collections = repository.findAllByOwnerId(user.getId());
        if (collections.isEmpty()) {
            var defaultCollection = new CardCollection();
            defaultCollection.setName(textProvider.getMessage("default_collection_name", user.getLanguage()));
            defaultCollection.setOwner(user);
            defaultCollection = repository.save(defaultCollection);
            user.getPayload().setDefaultCollection(defaultCollection.getId());
        }
    }

    public Page<CardCollection> getCollectionsPage(UUID id, int page) {
        var pageRequest = PageRequest.of(page, pageSize, Sort.by(Order.by("name")));
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

    public void initTutorialCollection(TelegramUser user, AvailableLocale collectionLanguage) {
        try (InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("default-collections/how-to.%s.json".formatted(collectionLanguage.getTag()))) {
            var map = objectMapper.readValue(
                inputStream, new TypeReference<LinkedHashMap<String, String>>() {
                }
            );

            var tutorialCollection = new CardCollection();
            tutorialCollection.setName(textProvider.getMessage("tutorial_collection_name", collectionLanguage));
            tutorialCollection.setOwner(user);
            tutorialCollection = repository.save(tutorialCollection);
            user.getPayload().setTutorialCollectionId(tutorialCollection.getId());

            var cards = new ArrayList<Card>();
            int appearDelay = 0;
            for (Entry<String, String> entry : map.entrySet()) {
                String question = entry.getKey();
                String answer = entry.getValue();
                var card = new Card();
                card.setQuestion(question);
                card.setAnswer(answer);
                card.setOwner(user);
                card.setCollection(tutorialCollection);
                card.setAppearTime(Instant.now().plus(appearDelay++, ChronoUnit.MILLIS));
                cards.add(card);
            }
            cardService.saveAll(cards);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(UUID lastChosenCollectionId) {
        return repository.existsById(lastChosenCollectionId);
    }

    public Optional<CardCollection> createCollection(String collectionName, TelegramUser user) {
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalArgumentException("Collection name cannot be empty");
        }
        if (getCollectionCount(user.getId()) >= collectionsLimit) {
            log.error("Collection couldn't be created, limit is reached for user {}", user.getId());
            return Optional.empty();
        }

        if (repository.existsByOwnerIdAndName(user.getId(), collectionName)) {
            return Optional.empty();
        }

        var collection = new CardCollection();
        collection.setName(collectionName);
        collection.setOwner(user);
        return Optional.of(repository.save(collection));
    }

    public boolean isLimitReached(UUID id) {
        boolean isReached = getCollectionCount(id) >= collectionsLimit;
        if (isReached) {
            log.error("Collection limit reached for user {}", id);
        }
        return isReached;
    }

    private Integer getCollectionCount(UUID id) {
        return repository.countAllByOwnerId(id);
    }
}
