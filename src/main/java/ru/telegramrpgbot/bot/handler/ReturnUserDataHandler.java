package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Slf4j
@Component
public class ReturnUserDataHandler implements Handler {

    private final IngameItemRepository ingameItemRepository;

    public ReturnUserDataHandler(IngameItemRepository ingameItemRepository) {
        this.ingameItemRepository = ingameItemRepository;
    }

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


        long temp = ingameItemRepository.findAllByUser(user).size();
        reply.setText(String.format("*%s* - *%s*%n" +
                        "\uD83D\uDCA0 Уровень: %s%n" +
                        "%s" +
                        "%n\uD83C\uDF1F Опыт: (%s/%s)%n" +
                        "♥️ Здоровье : %s/%s%n" +
                        "⚡️ Выносливость: %s/%s %s%n" +
                        "\uD83D\uDD39 Мана: %s/%s%n" +
                        "%n\uD83D\uDC8D Партнер: %s%n" +
                        "%n\uD83D\uDCB0 Золото: %s%n" +
                        "\uD83D\uDC8E Очки оффлайн ивентов: %s%n" +
                        "%n \uD83D\uDCE6 Инвентарь (x%d) - /Inventory",
                user.getUserClass().getName(),
                user.getName(),
                user.getLevel(),
                user.getPassivePoints() >0? "\n\uD83C\uDD99Очков пассивных умений: *"+user.getPassivePoints()+"* (/passives)\n" +
                        "" : "",
                user.getExp(),
                IngameUtil.countExpToLevel(user.getLevel()+1),
                user.getCurrentHealth(),
                user.getMaxHealth(),
                user.getCurrentStamina(),
                user.getMaxStamina(),
                user.getCurrentStamina() < user.getMaxStamina() ? "time: " + minutes +":"+seconds : "",
                user.getCurrentMana(),
                user.getMaxMana(),
                user.getPartner() != null ? user.getPartner() : "вы одиноки \uD83D\uDE22",
                //user.getClassId().getName(),
                user.getGold(),
                user.getOfflinePoints(),
                temp
                ));
        return List.of(reply);
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.HERO);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
