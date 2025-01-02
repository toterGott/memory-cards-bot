package com.example.memcards.card;

import com.example.memcards.collection.CardCollection;
import com.example.memcards.user.TelegramUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "card")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Card {

    @Id
    @GeneratedValue
    private UUID id;
    private String question; // todo jsonb
    private String answer; // todo jsonb
    private Instant appearTime;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser owner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = true)
    private CardCollection collection;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
