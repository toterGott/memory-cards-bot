package com.totergott.memcards.user;

import com.totergott.memcards.collection.CardCollection;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "telegram_user")
@Setter
@Getter
public class TelegramUser {

    @Id
    @GeneratedValue
    private UUID id;
    private Long chatId;
    @Enumerated(EnumType.STRING)
    private UserState state;
    private String username;
    private String firstName;
    private String lastName;
    @Enumerated(EnumType.STRING)
    private AvailableLocale language;
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb") // can be removed is ddl none
    private Payload payload = new Payload();
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CardCollection> collections = new ArrayList<>();
    private UUID currentCardId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focused_on_collection_id")
    private CardCollection focusedOnCollection;

    @Data
    @NoArgsConstructor
    public static class Payload {
        UUID defaultCollection;
        UUID lastChosenCollectionId;
        Instant lastChosenCollectionTimestamp;
        Schedule schedule;
        List<Integer> chatMessages = new ArrayList<>();

        @Data
        @NoArgsConstructor
        public static class Schedule {
            Integer hours;
            Instant nextRun;
        }
    }
}
