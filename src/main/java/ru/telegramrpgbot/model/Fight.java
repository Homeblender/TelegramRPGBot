package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "fight", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "user1_id")
    User user1Id;
    @ManyToOne
    @JoinColumn(name = "user2_id")
    @Builder.Default
    User user2Id = null;
    @Column(name = "fight_state")
    String fightState;



}
