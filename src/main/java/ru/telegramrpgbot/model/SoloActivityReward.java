package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "solo_activity_reward", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoloActivityReward{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "solo_activity_id")
    SoloActivity soloActivity;
    Long goldReward;
    Long expReward;
    @ManyToOne
    @JoinColumn(name = "item_reward")
    BaseItem itemReward;
    String resultMessage;
}