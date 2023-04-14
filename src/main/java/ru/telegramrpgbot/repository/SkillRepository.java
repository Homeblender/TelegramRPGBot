package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Skill;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
