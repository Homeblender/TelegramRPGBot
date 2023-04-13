package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.BotState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Entity
@Table(name = "applied_skill", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "skill_id")
    Skill skillId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User userId;
    @JoinColumn(name = "skill_level")
    Long skillLevel;
}