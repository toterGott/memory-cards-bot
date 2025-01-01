package com.example.memcards.collection;

import com.example.memcards.user.TelegramUser;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CardCollectionRepository cardCollectionRepository;

    public static final String DEFAULT_COLLECTION_NAME = "default";

    public void initDefaultCollection(TelegramUser user) {
        var collections = cardCollectionRepository.findAllByOwnerId(user.getId());
        if (collections.isEmpty()) {
            var defaultCollection = new CardCollection();
            defaultCollection.setName(DEFAULT_COLLECTION_NAME);
            defaultCollection.setOwner(user);
            cardCollectionRepository.save(defaultCollection);
        }
    }

    public CardCollection getDefaultCollection(UUID id) {
        return cardCollectionRepository.findByOwnerIdAndName(id, DEFAULT_COLLECTION_NAME).orElseGet(null);
    }

    public Page<CardCollection> getCollections(UUID id, Pageable pageable) {
        return cardCollectionRepository.findAllByOwnerId(id, pageable);
    }

    public Optional<CardCollection> findById(UUID id) {
        return cardCollectionRepository.findById(id);
    }
}
