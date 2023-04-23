package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.BaseItemCraft;
import ru.telegramrpgbot.model.Class;

import java.util.List;

@Repository
public interface BaseItemCraftRepository extends JpaRepository<BaseItemCraft, Long> {
    List<BaseItemCraft> findDistinctByCraftedBaseItemId_ClassRequired(Class craftedBaseItemId_classRequired);
    List<BaseItemCraft> findByCraftedBaseItemId(BaseItem craftedBaseItemId);
}
