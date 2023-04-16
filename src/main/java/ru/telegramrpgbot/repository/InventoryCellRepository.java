package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.InventoryCell;
import ru.telegramrpgbot.model.User;

import java.util.Collection;

@Repository
public interface InventoryCellRepository extends JpaRepository<InventoryCell, Long> {
    Collection<InventoryCell> findAllByUser(User user);
    InventoryCell findAllByInventoryCellId(Long inventoryCellId);
    Collection<InventoryCell> findAllByUserAndItem(User user, IngameItem item);
}
