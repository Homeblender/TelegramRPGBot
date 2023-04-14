package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "solo_activity", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoloActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(columnDefinition = "Text")
    String name;
    @Column(columnDefinition = "Text")
    String description;
    @JoinColumn(name = "state_name")
    @Column(columnDefinition = "Text")
    String stateName;
    @JoinColumn(name = "required_level")
    Long requiredLevel;
    @JoinColumn(name = "activity_duration")
    Long activityDuration;
}