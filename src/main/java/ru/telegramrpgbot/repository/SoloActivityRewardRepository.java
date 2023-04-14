package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.SoloActivityReward;

@Repository
public interface SoloActivityRewardRepository extends JpaRepository<SoloActivityReward, Long> {
}
