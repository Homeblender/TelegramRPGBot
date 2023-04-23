package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Class;

import java.util.Optional;
import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    @Override
    Optional<Class> findById(Long aLong);
    Optional<Class> findByName(String name);
    List<Class> findAllByBaseClass(Class baseClass);
}
