package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.model.Move;
import ru.telegramrpgbot.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    Optional<Move> getMoveByUserId(User user);
}
