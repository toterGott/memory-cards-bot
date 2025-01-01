package com.example.memcards.card;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findFirstByOwnerIdOrderByAppearTimeAsc(UUID ownerId);
}
