package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "activity_result_message", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoloActivityResultMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @JoinColumn(name = "solo_activity_id")
    @OneToOne
    SoloActivity soloActivityId;
    @JoinColumn(name = "result_message")
    @Column(columnDefinition = "Text")
    String resultMessage;
}