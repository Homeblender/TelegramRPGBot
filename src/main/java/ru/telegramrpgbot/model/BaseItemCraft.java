package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "base_item_craft", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseItemCraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "crafted_base_item_id")
    BaseItem craftedBaseItemId;
    @ManyToOne
    @JoinColumn(name = "material_base_item_id")
    BaseItem materialBaseItemId;
    Long countOfMaterial;


}