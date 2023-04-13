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
@Table(name = "solo_activity_reward", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoloActivityReward {
    @Id
    @JoinColumn(name = "solo_activity_id")
    @OneToOne
    SoloActivity soloActivityId;
    @JoinColumn(name = "gold_reward")
    Long goldReward;
    @JoinColumn(name = "exp_reward")
    Long expReward;
    @ManyToOne
    @JoinColumn(name = "item_reward")
    BaseItem itemReward;
}