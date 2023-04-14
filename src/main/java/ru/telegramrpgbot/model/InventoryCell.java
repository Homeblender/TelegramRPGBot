package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "inventory_cell", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCell implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long inventoryCellId;
    @OneToOne
    @JoinColumn(name = "item_id")
    @Unique
    //@Column(insertable = false, updatable = false, name = "item_id")
    IngameItem itemId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    User userId;
    @Builder.Default
    @Column(nullable = false)
    boolean isEquipped = false;
}