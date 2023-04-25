package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "consumable_item_effect", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumableItemEffect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @OneToOne
    @JoinColumn(name = "base_item_id")
    BaseItem baseItem;
    Long addLife;
    Long addMana;
    Long addStamina;



}
