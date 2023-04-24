package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "class", schema = "fixed")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Class {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(columnDefinition = "Text")
    String name;
    @Column(columnDefinition = "Text")
    String description;
    @JoinColumn(name = "required_level")
    Long requiredLevel;
    @ManyToOne
    @JoinColumn(name = "base_class")
    Class baseClass;
}
