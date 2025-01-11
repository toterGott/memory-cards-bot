package com.totergott.memcards.card;

import com.totergott.memcards.user.TelegramUser;
import java.util.ArrayList;
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
public class CardService {

    private final CardRepository repository;

    public Card save(Card card) {
        return repository.save(card);
    }

    public Card getCard(UUID currentCardId) {
        return repository.findById(currentCardId).orElse(null);
    }

    public Optional<Card> getCardToLearn(UUID userId) {
        return repository.findFirstByOwnerIdOrderByAppearTimeAsc(userId);
    }

    public Optional<Card> getCardToLearn(UUID userId, UUID collectionId) {
        return repository.findFirstByOwnerIdAndCollectionIdOrderByAppearTimeAsc(userId, collectionId);
    }

    public Optional<Card> findById(UUID uuid) {
        return repository.findById(uuid);
    }

    public void deleteById(UUID cardId) {
        repository.deleteById(cardId);
    }

    public Optional<Card> getCardToLearn(TelegramUser user) {
        if (user.getFocusedOnCollection() != null) {
            return repository.findAvailableCardInCollection(
                user.getId(),
                user.getFocusedOnCollection().getId()
            );
        }
        return repository.findAvailableCard(user.getId());
    }

    public Page<Card> getCardPageByCollectionId(UUID collectionId, int pageIdx) {
        var pageRequest = PageRequest.of(pageIdx, 3, Sort.by(Order.desc("createdAt")));
        return repository.findAllByCollectionId(collectionId, pageRequest);
    }

    public void saveAll(ArrayList<Card> cards) {
        repository.saveAll(cards);
    }
}
