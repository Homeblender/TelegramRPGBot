package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;

import java.util.List;

@Repository
public interface IngameItemRepository extends JpaRepository<IngameItem, Long> {
    List<IngameItem> findAllByUser(User user);
    IngameItem findAllById(Long id);
}
