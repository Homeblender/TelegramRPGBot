package ru.telegramrpgbot.bot.handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telegramrpgbot.enums.BotState;
import ru.telegramrpgbot.enums.Command;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static ru.telegramrpgbot.util.TelegramUtil.createMessageTemplate;
@Slf4j
@Component
public class ReturnUserDataHandler implements Handler{

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        log.info("me");
        var reply = createMessageTemplate(user);
        reply.setText(String.format("%s\n" +
                "level: %s\n" +
                "passive points: %s\n" +
                "XP: %s\n" +
                "HP: %s/%s\n" +
                "Stamina: %s/%s %s\n" +
                "Mana: %s/%s\n" +
                "Partner: %s\n" +
                //"class: %s\n" +
                "gold: %s\n" +
                "offline points: %s\n",
                user.getName(),
                user.getLevel(),
                user.getPassivePoints(),
                user.getCurrentExp(),
                user.getCurrentHealth(),
                user.getMaxHealth(),
                user.getCurrentStamina(),
                user.getMaxStamina(),
                user.getCurrentStamina() < user.getMaxStamina()? "time: " + user.getStaminaRestor() : "",
                user.getCurrentMana(),
                user.getMaxMana(),
                user.getPartner() != null? user.getPartner() : "",
                //user.getClassId().getName(),
                user.getGold(),
                user.getOfflinePoints()));
        log.info("me");
        return List.of(reply);
    }
    @Override
    public BotState operatedBotState() {
        return null;
    }

    @Override
    public Command operatedCommand() {
        return Command.ME;
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return Collections.emptyList();
    }
}
