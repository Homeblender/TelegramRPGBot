package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.BodyPart;
import ru.telegramrpgbot.bot.enums.MoveState;

import javax.persistence.*;

@Entity
@Table(name = "move", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Move {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User userId;
    @ManyToOne
    @JoinColumn(name = "fight_id")
    Fight fightId;
    @Column(name = "defense")
    @Builder.Default
    BodyPart defense = null;
    @Column(name = "attack")
    @Builder.Default
    BodyPart attack = null;
    @Column(name = "move_state")
    @Builder.Default
    MoveState moveState = MoveState.NEW_MOVE;
    @Builder.Default
    Long hp = 100L;
    @Builder.Default
    Long num = 0L;
}
