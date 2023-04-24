package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.EventState;
import ru.telegramrpgbot.bot.enums.EventType;

import javax.persistence.*;

@Entity
@Table(name = "offline_event", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "creator")
    User creator;
    @Builder.Default
    String eventName = "CREATING";
    @Builder.Default
    String eventGoal = "CREATING";
    @Builder.Default
    Long offlinePointsReward = 0L;
    @Builder.Default
    @Column(columnDefinition = "integer")
    EventType eventType = EventType.USER;
    @Builder.Default
    @Column(columnDefinition = "integer")
    EventState eventState = EventState.CREATING;
}