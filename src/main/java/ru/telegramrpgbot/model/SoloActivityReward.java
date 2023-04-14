package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;
import ru.telegramrpgbot.bot.BotState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "solo_activity_reward", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoloActivityReward implements Serializable {
    @Id
    @JoinColumn(name = "solo_activity_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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