package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.ItemType;

@Repository
public interface ItemTypeRepository extends JpaRepository<ItemType, Long> {
}