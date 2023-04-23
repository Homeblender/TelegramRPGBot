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
        if (message.split("_").length==1){
            return baseGuide(user);
        }else if (Objects.equals(message.split("_")[1].toUpperCase(), "RAIDS")){
            return raidsGuide(user);
        }else if (Objects.equals(message.split("_")[1].toUpperCase(), "EVENTS")){
            return eventsGuide(user);
        }else if (Objects.equals(message.split("_")[1].toUpperCase(), "WEDDINGS")){
            return weddingsGuide(user);
        }else if (Objects.equals(message.split("_")[1].toUpperCase(), "FIGHTS")){
            return fightsGuide(user);
        }
        return baseGuide(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> fightsGuide(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Игрок может вызвать на дуэль другого игрока командой командой \n/fight\\_[имя]\\_[ставка золота].\n" +
                "В ходе сражения игроки будут одновременно выбирать часть тела для защиты либо атаки.\n" +
                "Вместо атаки игрок может потратить ману для применения особых умений своего класса.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> weddingsGuide(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Игрок может сделать предложение другому игроку командой \n/propose\\_[имя].\n" +
                "Чтобы развестись нужно написать команду \n/divorce.\n");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> eventsGuide(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Любой игрок может создать событие от имени игрока, но для этого необходимо будет потратить очки оффлайн событий.\n" +
                "Игрок, который добавил бота в группу, становится администратором и может создавать события от имени администритора.\n" +
                "События можно отменять, либо завершать, при завершении события обязательно указывать никнейм игрока который победил.\n");
        return List.of(reply);
    }


    private List<PartialBotApiMethod<? extends Serializable>> raidsGuide(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Для атаки на босса все участники группы не должны быть заняты.\n" +
                "Класс *Воин* и его подклассы будут принимать на себя удар первыми.\n" +
                "Босс будет атаковать случайного игрока\n" +
                "Хотя в рейд можно пойти и в одиночку, рекомендуется собрать полную группу.");
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> baseGuide(User user) {
        var reply = createMessageTemplate(user);

        reply.setText("Дополнительная информация:\n\n" +
                "*Рейды* - /guide\\_raids\n" +
                "*Сражения* - /guide\\_fights\n" +
                "*События* - /guide\\_events\n" +
                "*Свадьбы* - /guide\\_weddings\n");
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
