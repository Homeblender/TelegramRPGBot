package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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
    Skill skill;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    @JoinColumn(name = "skill_level")
    Long skillLevel;
}