package com.example.memcards.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<TelegramUser, UUID> {

    Optional<TelegramUser> findByChatId(Long chatId);
}
