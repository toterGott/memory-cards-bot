package com.totergott.memcards.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID> {

    @Query(
        nativeQuery = true,
        value = """
            select * from card
            where user_id = :ownerId
                and (archived != true or archived is null)
            order by appear_time
            limit 1
            """
    )
    Optional<Card> findFirstByOwnerIdAndArchivedIsFalseOrderByAppearTimeAsc(@Param("ownerId") UUID ownerId);

    @Query(
        nativeQuery = true,
        value = """
            select * from card
            where user_id = :ownerId
                and (archived != true or archived is null)
                and collection_id = :collectionId
            order by appear_time
            limit 1
            """
    )
    Optional<Card> findFirstByOwnerIdAndCollectionIdAndArchivedIsFalseOrderByAppearTimeAsc(@Param("ownerId") UUID ownerId, @Param("collectionId") UUID collectionId);

    @Query(
        nativeQuery = true,
        value = """
            select * from card
            where user_id = :ownerId
                    and collection_id = :collectionId
                    and appear_time < now()
                    and (archived != true or archived is null)
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
                    and (archived != true or archived is null)
            order by appear_time
            limit 1
            """
    )
    Optional<Card> findAvailableCard(@Param("ownerId") UUID ownerId);

    Page<Card> findAllByCollectionId(UUID collectionId, Pageable pageable);

    Integer countAllByOwnerId(UUID id);
}
