package com.totergott.memcards.user;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;

    public Optional<TelegramUser> getUserByTelegramId(Long chatId) {
        var user = repository.findByChatId(chatId);
        user.ifPresentOrElse(
            foundTelegramUser -> log.debug("Found user {}", foundTelegramUser),
            () -> log.debug("User not found: {}", chatId)
        );
        return user;
    }

    public TelegramUser createUser(Chat chat, String languageCode) {
        var user = new TelegramUser();
        user.setChatId(chat.getId());
        user.setUsername(chat.getUserName());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setState(UserState.STAND_BY);
        user.setLanguage(resolveLanguageCode(languageCode));
        user = repository.save(user);
        log.debug("Created user: {}", user);
        return user;
    }

    private AvailableLocale resolveLanguageCode(String languageCode) {
        if (languageCode == null) {
            return AvailableLocale.EN;
        }
        if ("ru".equalsIgnoreCase(languageCode)) {
            return AvailableLocale.RU;
        }
        return AvailableLocale.EN;
    }

    public TelegramUser updateUserInfoIfNeeded(TelegramUser user, Chat chat) {
        boolean updateRequired = false;
        if (!Objects.equals(user.getFirstName(), chat.getFirstName())) {
            user.setFirstName(chat.getFirstName());
            updateRequired = true;
        }
        if (!Objects.equals(user.getLastName(), chat.getLastName())) {
            user.setLastName(chat.getLastName());
            updateRequired = true;
        }
        if (!Objects.equals(user.getUsername(), chat.getUserName())) {
            user.setUsername(chat.getUserName());
            updateRequired = true;
        }
        if (updateRequired) {
            user = repository.save(user);
        }
        return user;
    }

    public TelegramUser save(TelegramUser user) {
        return repository.save(user);
    }

    public Optional<TelegramUser> getScheduledUser() {
        return repository.getScheduledUser(List.of(
            UserState.STAND_BY.name(),
            UserState.QUESTION_SHOWED.name(),
            UserState.EVALUATE_ANSWER.name()
        ));
    }

    public List<TelegramUser> getActiveUsers() {
        return repository.getActiveUsers(3);
    }
}
