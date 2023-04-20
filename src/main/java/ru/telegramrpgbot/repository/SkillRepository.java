package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.Skill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findAllByClassId(Class classes);
    List<Skill> findAllByClassIdIn(Collection<Class> classId);
    @Override
    Optional<Skill> findById(Long id);
}
