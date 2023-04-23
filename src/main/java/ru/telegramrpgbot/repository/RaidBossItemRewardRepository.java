package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.RaidBossItemReward;

@Repository
public interface RaidBossItemRewardRepository extends JpaRepository<RaidBossItemReward, Long> {
}
