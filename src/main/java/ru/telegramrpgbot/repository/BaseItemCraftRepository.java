package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.AppliedSkill;
import ru.telegramrpgbot.model.BaseItemCraft;

@Repository
public interface BaseItemCraftRepository extends JpaRepository<BaseItemCraft, Long> {
}
