package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.AppliedSkill;
import ru.telegramrpgbot.model.Skill;
import ru.telegramrpgbot.model.User;

import java.util.Optional;
import java.util.List;

@Repository
public interface AppliedSkillRepository extends JpaRepository<AppliedSkill, Long> {
    Optional<AppliedSkill> findAllBySkillAndUser(Skill skill, User user);
    List<AppliedSkill> findAllByUser(User user);

}
