package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.AppliedSkill;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.ConsumableItemEffect;

import java.util.Optional;

@Repository
public interface ConsumableItemEffectRepository extends JpaRepository<ConsumableItemEffect, Long> {

    Optional<ConsumableItemEffect> findByBaseItem(BaseItem baseItem);
}
