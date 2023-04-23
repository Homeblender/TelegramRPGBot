package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.RaidBoss;
import ru.telegramrpgbot.model.RaidBossItemReward;

import java.util.List;

@Repository
public interface RaidBossItemRewardRepository extends JpaRepository<RaidBossItemReward, Long> {
    List<RaidBossItemReward> findByBoss(RaidBoss boss);
}
