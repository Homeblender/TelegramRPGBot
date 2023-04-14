package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.SoloActivityResultMessage;

@Repository
public interface SoloActivityResultMessageRepository extends JpaRepository<SoloActivityResultMessage, Long> {
}
