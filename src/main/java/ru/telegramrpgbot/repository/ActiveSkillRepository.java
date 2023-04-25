package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.ActiveSkill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveSkillRepository extends JpaRepository<ActiveSkill, Long> {

    List<ActiveSkill> findAllByClassId(Class classes);
    List<ActiveSkill> findAllByClassIdIn(Collection<Class> classId);
    @Override
    Optional<ActiveSkill> findById(Long id);

}