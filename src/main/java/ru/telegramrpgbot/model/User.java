package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.repository.ClassRepository;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "usr", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    Long chatId;
    @Builder.Default
    String name = null;
    @Builder.Default
    Long level = 1L;
    @Builder.Default
    Long passivePoints = 0L;
    @Builder.Default
    Long exp = 0L;
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
    @Builder.Default
    Timestamp activityEnds = null;
    @Builder.Default
    @OneToOne
    @JoinColumn(name = "activity_id")
    SoloActivity activityId = null;
    @Builder.Default
    Timestamp staminaRestor = null;
    @Builder.Default
    @Column(columnDefinition = "integer")
    BotState userState = BotState.START;
    @OneToOne
    @JoinColumn(name = "partner_chat_id")
    User partner = null;
    @ManyToOne
    @JoinColumn(name = "class_id")
    Class userClass = null;
    @Builder.Default
    Long gold = 0L;
    @Builder.Default
    Long offlinePoints = 0L;

}
