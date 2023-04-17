package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.BodyPart;
import ru.telegramrpgbot.bot.enums.BotState;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

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
    @Column(name = "user1_def")
    @Builder.Default
    BodyPart user1Def = null;
    @Column(name = "user2_def")
    @Builder.Default
    BodyPart user2Def = null;
    @Column(name = "user1_attack")
    @Builder.Default
    BodyPart user1Attack = null;
    @Column(name = "user2_attack")
    @Builder.Default
    BodyPart user2Attack = null;
    @Column(name = "move_num")
    Long moveNum;

}
