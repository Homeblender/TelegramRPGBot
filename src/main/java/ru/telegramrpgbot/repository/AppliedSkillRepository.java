package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.AppliedSkill;

@Repository
public interface AppliedSkillRepository extends JpaRepository<AppliedSkill, Long> {
}
