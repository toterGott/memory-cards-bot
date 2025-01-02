package com.example.memcards.collection;

import com.example.memcards.card.Card;
import com.example.memcards.common.PageableEntity;
import com.example.memcards.user.TelegramUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "collection")
@Getter
@Setter
public class CardCollection implements PageableEntity {

    @Id
    @GeneratedValue
    private UUID id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private TelegramUser owner;
    @OneToMany(
        mappedBy = "collection",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<Card> cards = new ArrayList<>();
}
