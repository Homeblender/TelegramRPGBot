package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.ItemType;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;

import java.util.List;

@Repository
public interface IngameItemRepository extends JpaRepository<IngameItem, Long> {
    List<IngameItem> findAllByUser(User user);
    List<IngameItem> findAllByUserAndBaseItem_Type(User user, ItemType baseItem_type);
    List<IngameItem> findAllByUserAndBaseItem(User user, BaseItem baseItem);
    IngameItem findAllByIdAndUser(Long id, User user);
}
