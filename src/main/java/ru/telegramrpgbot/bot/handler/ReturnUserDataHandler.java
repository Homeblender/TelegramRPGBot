package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Slf4j
@Component
public class ReturnUserDataHandler implements Handler {

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());

        var time = user.getStaminaRestor() == null ? 0 : user.getStaminaRestor().getTime() - System.currentTimeMillis();
        long secondsTmp = TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
        long minutes = secondsTmp/60;
        secondsTmp = secondsTmp%60;
        String seconds = Long.toString(secondsTmp);
        if (seconds.length()==1)
            seconds = "0"+seconds;



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
                user.getExp(),
                user.getCurrentHealth(),
                user.getMaxHealth(),
                user.getCurrentStamina(),
                user.getMaxStamina(),
                user.getCurrentStamina() < user.getMaxStamina() ? "time: " + minutes +":"+seconds : "",
                user.getCurrentMana(),
                user.getMaxMana(),
                user.getPartner() != null ? user.getPartner() : "",
                //user.getClassId().getName(),
                user.getGold(),
                user.getOfflinePoints()));
        return List.of(reply);
    }

    @Override
    public BotState operatedBotState() {
        return null;
    }

    @Override
    public Command operatedCommand() {
        return Command.HERO;
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return Collections.emptyList();
    }
}
