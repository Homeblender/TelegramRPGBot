package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.model.Fight;
import ru.telegramrpgbot.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FightRepository extends JpaRepository<Fight, Long> {
    Optional<Fight> getFightByUser1Id(User user1Id);
    Optional<Fight> getFightByUser2Id(User user2Id);
}