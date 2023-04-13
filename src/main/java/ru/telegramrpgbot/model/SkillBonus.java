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
@Table(name = "skill_bonus", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillBonus {
    @Id
    @JoinColumn(name = "skill_id")
    @OneToOne
    Skill id;
    @JoinColumn(name = "damage_bonus")
    Long damageBonus;
    @JoinColumn(name = "armor_bonus")
    Long armorBonus;
    @JoinColumn(name = "health_bonus")
    Long healthBonus;
    @JoinColumn(name = "mana_bonus")
    Long manaBonus;

}