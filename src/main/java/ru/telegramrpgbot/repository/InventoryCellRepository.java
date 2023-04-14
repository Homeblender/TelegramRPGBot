package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.InventoryCell;

@Repository
public interface InventoryCellRepository extends JpaRepository<InventoryCell, Long> {
}
