package com.totergott.memcards.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<TelegramUser, UUID> {

    Optional<TelegramUser> findByChatId(Long chatId);

    @Query(
        nativeQuery = true,
        value = """
            select *
            from telegram_user
            where state in (:states)
              and payload -> 'schedule' is not null
              and (payload -> 'schedule' ->> 'nextRun')::timestamptz < now()
            order by (payload -> 'schedule' ->> 'nextRun')::timestamptz
            limit 1
            """
    )
    Optional<TelegramUser> getScheduledUser(@Param("states") List < String > states);
}
