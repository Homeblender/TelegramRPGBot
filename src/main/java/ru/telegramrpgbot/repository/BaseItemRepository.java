package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.BaseItem;

@Repository
public interface BaseItemRepository extends JpaRepository<BaseItem, Long> {
}
