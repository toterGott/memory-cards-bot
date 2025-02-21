package com.totergott.memcards.collection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CardCollectionRepository extends JpaRepository<CardCollection, UUID> {

    List<CardCollection> findAllByOwnerId(UUID userId);

    Optional<CardCollection> findByOwnerIdAndName(UUID ownerId, String name);

    Page<CardCollection> findAllByOwnerId(UUID ownerId, Pageable pageable);

    Integer countAllByOwnerId(UUID ownerId);

    boolean existsByOwnerIdAndName(UUID ownerId, String name);

    @Query(nativeQuery = true,
        value = """
            delete from collection where id = :id
        """)
    @Modifying
    void deleteByIdQuery(UUID id);
}
