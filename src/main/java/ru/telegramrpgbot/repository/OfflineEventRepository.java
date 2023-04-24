package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.bot.enums.EventState;
import ru.telegramrpgbot.bot.enums.EventType;
import ru.telegramrpgbot.model.OfflineEvent;
import ru.telegramrpgbot.model.User;

import java.util.List;

@Repository
public interface OfflineEventRepository extends JpaRepository<OfflineEvent, Long> {
    List<OfflineEvent> findAllByEventTypeAndEventState(EventType eventType, EventState eventState);
    OfflineEvent findByCreatorAndEventState(User creator, EventState eventState);

    OfflineEvent findById(long parseLong);
}
