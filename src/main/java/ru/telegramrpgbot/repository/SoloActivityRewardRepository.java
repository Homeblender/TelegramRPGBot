package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.SoloActivity;
import ru.telegramrpgbot.model.SoloActivityReward;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoloActivityRewardRepository extends JpaRepository<SoloActivityReward, Long> {
    List<SoloActivityReward> findAllBySoloActivity(SoloActivity soloActivity);
}
