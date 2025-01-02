package com.example.memcards.card;

import com.example.memcards.user.TelegramUser;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;

    public Card save(Card card) {
        return cardRepository.save(card);
    }

    public Card getCard(UUID currentCardId) {
        return cardRepository.findById(currentCardId).orElse(null);
    }

    public Optional<Card> getCardToLearn(UUID userId) {
        return cardRepository.findFirstByOwnerIdOrderByAppearTimeAsc(userId);
    }

    public Optional<Card> getCardToLearn(UUID userId, UUID collectionId) {
        return cardRepository.findFirstByOwnerIdAndCollectionIdOrderByAppearTimeAsc(userId, collectionId);
    }

    public Optional<Card> findById(UUID uuid) {
        return cardRepository.findById(uuid);
    }
}
