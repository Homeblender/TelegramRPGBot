package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class GuideHandler implements Handler {

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (Objects.equals(message.split("_")[1].toUpperCase(), "RAID")){
            return raidGuide(user);
        }
        return null;
    }

    private List<PartialBotApiMethod<? extends Serializable>> raidGuide(User user) {
        var reply = createMessageTemplate(user);

        reply.setText("Для атаки на босса все участники группы не должны быть заняты.\n" +
                "Класс *Воин* и его подклассы будут принимать на себя удар первыми.\n" +
                "Босс будет атаковать случайного игрока\n" +
                "Хотя в рейд можно пойти и в одиночку, рекомендуется собрать полную группу.");
        return List.of(reply);
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.GUIDE);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
