package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.ItemType;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.Class;

import java.util.List;

@Repository
public interface BaseItemRepository extends JpaRepository<BaseItem, Long> {
    List<BaseItem> findAllByClassRequiredAndIsForSaleAndType(Class classRequired, Boolean isForSale, ItemType type);
    BaseItem findAllById(Long id);
}
