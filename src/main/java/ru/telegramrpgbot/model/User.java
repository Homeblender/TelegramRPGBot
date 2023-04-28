package ru.telegramrpgbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.telegramrpgbot.bot.enums.BotState;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "usr", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    Long chatId;
    @Builder.Default
    String name = null;
    @Builder.Default
    Long level = 1L;
    @Builder.Default
    Long passivePoints = 0L;
    @Builder.Default
    Long exp = 0L;
    @Builder.Default
    Long currentHealth = 100L;
    @Builder.Default
    Long maxHealth = 100L;
    @Builder.Default
    Long currentStamina = 10L;
    @Builder.Default
    Long maxStamina = 10L;
    @Builder.Default
    Long currentMana = 100L;
    @Builder.Default
    Long maxMana = 100L;
    @Builder.Default
    Timestamp activityEnds = null;
    @Builder.Default
    @OneToOne
    @JoinColumn(name = "activity_id")
    SoloActivity activityId = null;
    @Builder.Default
    Timestamp staminaRestor = null;
    @Builder.Default
    @Column(columnDefinition = "integer")
    BotState userState = BotState.START;
    @OneToOne
    @JoinColumn(name = "partner_chat_id")
    User partner = null;
    @ManyToOne
    @JoinColumn(name = "party_id")
    Party partyId;
    @OneToOne
    @JoinColumn(name = "host_party_id")
    Party hostPartyId;
    @ManyToOne
    @JoinColumn(name = "class_id")
    Class userClass = null;
    @Builder.Default
    Long gold = 0L;
    @Builder.Default
    Boolean isGameMaster = false;
    @Builder.Default
    Long offlinePoints = 0L;

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof User))
            return false;
        User otherA = (User) other;
        return
                (chatId.equals(otherA.chatId)) && ((exp == null)
                        ? otherA.exp == null
                        : exp.equals(otherA.exp));
    }
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + chatId.hashCode();
        hash = hash * 31
                + (exp == null ? 0 : exp.hashCode());
        return hash;
    }

}
