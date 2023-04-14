package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Party;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
}
