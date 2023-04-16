package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "ingame_item", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngameItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "item_id")
    BaseItem baseItem;
    Long sharpness;
    Long itemsInStack;
}