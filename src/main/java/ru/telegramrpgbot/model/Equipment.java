package ru.telegramrpgbot.model;

import javax.persistence.*;

//@Entity
//@Table(name = "equipment", schema = "public")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long equipmentId;
    @OneToOne
    @JoinColumn(name = "user_id")
    User userId;
    @OneToOne
    @JoinColumn(name = "helmet", referencedColumnName = "inventoryCellId")
    InventoryCell helmet;
    @OneToOne
    @JoinColumn(name = "chest_armor", referencedColumnName = "inventoryCellId")
    InventoryCell chestArmor;
    @OneToOne
    @JoinColumn(name = "leg_armor", referencedColumnName = "inventoryCellId")
    InventoryCell legArmor;
    @OneToOne
    @JoinColumn(name = "boots", referencedColumnName = "inventoryCellId")
    InventoryCell boots;
    @OneToOne
    @JoinColumn(name = "left_hand", referencedColumnName = "inventoryCellId")
    InventoryCell leftHand;
    @OneToOne
    @JoinColumn(name = "helmet", referencedColumnName = "inventoryCellId")
    InventoryCell rightHand;
}