package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.SoloActivity;

@Repository
public interface SoloActivityRepository extends JpaRepository<SoloActivity, Long> {
}