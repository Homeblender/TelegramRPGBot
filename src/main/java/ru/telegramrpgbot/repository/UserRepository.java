package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.enums.BotState;
import ru.telegramrpgbot.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserByChatId(Long chatId);
    Optional<User> getUsersByName(String name);
    List<User> findAllByUserState(BotState currentUserState);
}
