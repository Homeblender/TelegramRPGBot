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
@Table(name = "base_item", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(columnDefinition = "Text")
    String name;
    @Column(columnDefinition = "Text")
    String description;
    Long damage;
    Long armor;
    @ManyToOne
    @JoinColumn(name = "type_id")
    ItemTypes typeId;
    @JoinColumn(name = "buy_price")
    Long buyPrice;
    @JoinColumn(name = "sell_price")
    Long sellPrice;
}