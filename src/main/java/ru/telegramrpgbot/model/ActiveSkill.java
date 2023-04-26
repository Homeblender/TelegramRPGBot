package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "active_skill", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(columnDefinition = "Text")
    String name;
    @ManyToOne
    @JoinColumn(name = "class_id")
    Class classId;
    @Column(name = "damage_bonus")
    Double damageBonus;
    @Column(name = "mana_cost")
    Long manaCost;

}
