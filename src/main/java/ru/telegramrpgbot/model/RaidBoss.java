package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "raid_boss", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaidBoss {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    Long life;
    Long damage;
    Long armor;
    Long goldReward;
    Long expReward;
}