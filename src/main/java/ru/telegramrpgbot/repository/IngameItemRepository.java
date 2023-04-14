package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.IngameItem;

@Repository
public interface IngameItemRepository extends JpaRepository<IngameItem, Long> {
}
