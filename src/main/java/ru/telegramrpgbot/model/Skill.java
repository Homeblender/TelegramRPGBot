package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "skill", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(columnDefinition = "Text")
    String name;
    @ManyToOne
    @JoinColumn(name = "class_id")
    Class classId;
    Long damageBonus;
    Long armorBonus;
    Long healthBonus;
    Long manaBonus;

}