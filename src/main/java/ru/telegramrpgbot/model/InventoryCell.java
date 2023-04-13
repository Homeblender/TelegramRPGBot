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
@Table(name = "inventory_cell", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCell {
    @Id
    @OneToOne
    @JoinColumn(name = "item_id")
    IngameItem itemId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User userId;
}