package com.example.memcards.collection;

import com.example.memcards.i18n.MessageProvider;
import com.example.memcards.user.TelegramUser;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CardCollectionRepository repository;
    private final MessageProvider messageProvider;

    public void initDefaultCollection(TelegramUser user) {
        var collections = repository.findAllByOwnerId(user.getId());
        if (collections.isEmpty()) {
            var defaultCollection = new CardCollection();
            defaultCollection.setName(messageProvider.getMessage("default_collection_name", user.getLanguage()));
            defaultCollection.setOwner(user);
            defaultCollection = repository.save(defaultCollection);
            user.getPayload().setDefaultCollection(defaultCollection.getId());
        }

        for (int i = 0; i < 9; i++) {
            var stubCollection = new CardCollection();
            stubCollection.setName("Stub collection " + i);
            stubCollection.setOwner(user);
            repository.save(stubCollection);
        }
    }

    public Page<CardCollection> getCollectionsPage(UUID id, int page) {
        var pageRequest = PageRequest.of(page, 3, Sort.by(Order.by("name")));
        return repository.findAllByOwnerId(id, pageRequest);
    }

    public Optional<CardCollection> findById(UUID id) {
        return repository.findById(id);
    }

    public int countUserCollections(UUID ownerId) {
        return repository.countAllByOwnerId(ownerId);
    }

    public Optional<CardCollection> getById(UUID defaultCollection) {
        return repository.findById(defaultCollection);
    }

    public void deleteById(UUID collectionId) {
        repository.deleteByIdQuery(collectionId);
    }
}
