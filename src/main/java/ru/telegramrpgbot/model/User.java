package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.enums.BotState;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "usr", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    Long chatId;
    String name;
    @Builder.Default
    Long level = 1L;
    @Builder.Default
    Long passivePoints = 0L;
    @Builder.Default
    Long currentExp = 0L;
    @Builder.Default
    Long currentHealth = 100L;
    @Builder.Default
    Long maxHealth = 100L;
    @Builder.Default
    Long currentStamina = 10L;
    @Builder.Default
    Long maxStamina = 10L;
    @Builder.Default
    Long currentMana = 100L;
    @Builder.Default
    Long maxMana = 100L;
    Timestamp lastAction = null;
    Timestamp staminaRestor = null;
    @Builder.Default
    @Column(columnDefinition = "Text")
    BotState currentUserState = BotState.START;
    @OneToOne
    @JoinColumn(name = "partner_chat_id")
    User partner = null;
    @ManyToOne
    @JoinColumn(name = "class_id")
    Class classId = null;
    @Builder.Default
    Long gold = 0L;
    @Builder.Default
    Long offlinePoints = 0L;
}
