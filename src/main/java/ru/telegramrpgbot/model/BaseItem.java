package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.ItemType;

import javax.persistence.*;
import java.util.Collection;

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
    ItemType type;
    Long buyPrice;
    Long maxInStack;
    Boolean isForSale;
    @JoinColumn(name = "class_required_id")
    @ManyToOne
    Class classRequired;
    @OneToMany(mappedBy = "materialBaseItemId")
    Collection<BaseItemCraft> baseItemsToCraft;

}