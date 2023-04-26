package ru.telegramrpgbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.telegramrpgbot.model.Party;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
    Optional<Party> getPartyByName(String name);
    List<Party> findAllByBossFightingNotNull();
}
