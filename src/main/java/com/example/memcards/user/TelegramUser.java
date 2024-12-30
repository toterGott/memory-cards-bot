package com.example.memcards.user;

import com.example.memcards.collection.CardCollection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "telegram_user")
@Setter
@Getter// todo fix
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
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CardCollection> collections = new ArrayList<>();
    private UUID currentCardId;
}
