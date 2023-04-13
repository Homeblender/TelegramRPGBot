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
@Table(name = "class")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Class {
    @Id
    Long id;
    String name;
    String description;
    Long required_level;
    @OneToOne
    @JoinColumn(name = "base_class")
    Class baseClass;
}
