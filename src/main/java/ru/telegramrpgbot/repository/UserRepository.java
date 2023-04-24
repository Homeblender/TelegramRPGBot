package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.model.Party;
import ru.telegramrpgbot.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserByChatId(Long chatId);
    Optional<User> getUsersByName(String name);
    Optional<User> findUserByHostPartyId(Party party);
    List<User> findAllByUserState(BotState currentUserState);
    List<User> findAllByPartyId(Party party);
    Optional<User> findAllByName(String name);
}
