package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.RaidBoss;

@Repository
public interface RaidBossRepository extends JpaRepository<RaidBoss, Long> {
}
