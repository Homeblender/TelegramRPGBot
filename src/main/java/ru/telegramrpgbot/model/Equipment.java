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
@Table(name = "equipment", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {
    @Id
    @OneToOne
    @JoinColumn(name = "user_id")
    User user_id;
    @OneToOne
    @JoinColumn(name = "helmet")
    InventoryCell helmet;
    @OneToOne
    @JoinColumn(name = "chest_armor")
    InventoryCell chestArmor;
    @OneToOne
    @JoinColumn(name = "leg_armor")
    InventoryCell legArmor;
    @OneToOne
    @JoinColumn(name = "boots")
    InventoryCell boots;
    @OneToOne
    @JoinColumn(name = "left_hand")
    InventoryCell leftHand;
    @OneToOne
    @JoinColumn(name = "helmet")
    InventoryCell rightHand;
}