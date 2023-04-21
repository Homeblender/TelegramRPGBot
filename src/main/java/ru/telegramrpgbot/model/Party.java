package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "party", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long partyId;
    @Column(columnDefinition = "Text")
    String name;
    @Column(columnDefinition = "Text")
    String current_state;
}