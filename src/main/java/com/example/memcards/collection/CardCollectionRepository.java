package com.example.memcards.collection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCollectionRepository extends JpaRepository<CardCollection, UUID> {

    List<CardCollection> findAllByOwnerId(UUID userId);

    Optional<CardCollection> findByOwnerIdAndName(UUID ownerId, String name);

    Page<CardCollection> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
