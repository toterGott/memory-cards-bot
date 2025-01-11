package com.totergott.memcards.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findFirstByOwnerIdOrderByAppearTimeAsc(UUID ownerId);
    Optional<Card> findFirstByOwnerIdAndCollectionIdOrderByAppearTimeAsc(UUID ownerId, UUID collectionId);

    @Query(
        nativeQuery = true,
        value = """
            select * from card
            where user_id = :ownerId
                    and collection_id = :collectionId
                    and appear_time < now()
            order by appear_time
            limit 1
            """
    )
    Optional<Card> findAvailableCardInCollection(UUID ownerId, UUID collectionId);

    @Query(
        nativeQuery = true,
        value = """
            select * from card
                where user_id = :ownerId
                    and appear_time < now()
            order by appear_time
            limit 1
            """
    )
    Optional<Card> findAvailableCard(UUID ownerId);

    Page<Card> findAllByCollectionId(UUID collectionId, Pageable pageable);
}
