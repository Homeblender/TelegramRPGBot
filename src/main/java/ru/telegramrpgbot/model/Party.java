package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.BotState;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Entity
@Table(name = "party", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {
    @Id
    @OneToOne
    @JoinColumn(name = "user_id")
    User userId;
    @Column(columnDefinition = "Text")
    String name;
    @Column(columnDefinition = "Text")
    String current_state;
}