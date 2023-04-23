package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "raid_boss_item_reward", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaidBossItemReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "item_id")
    BaseItem item;
    @ManyToOne
    @JoinColumn(name = "boss_id")
    RaidBoss boss;
}