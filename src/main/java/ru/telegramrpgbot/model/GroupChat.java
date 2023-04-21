package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "group_chat", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupChat {
    @Id
    Long id;
    @ManyToOne
    @JoinColumn(name = "user_invited")
    User user;

}