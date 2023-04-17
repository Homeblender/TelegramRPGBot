package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.BodyPart;
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
    @JoinColumn(name = "defense")
    @Builder.Default
    BodyPart defense = null;
    @JoinColumn(name = "attack")
    @Builder.Default
    BodyPart attack = null;
    @Builder.Default
    Long num = 0L;
}
