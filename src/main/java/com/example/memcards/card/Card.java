package com.example.memcards.card;

import com.example.memcards.collection.CardCollection;
import com.example.memcards.common.PageableEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "card")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Card implements PageableEntity {

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "collection_id")
    private CardCollection collection;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Override
    public String getName() {
        return question;
    }
}
