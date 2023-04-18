package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class CityCommandsHandler implements Handler {

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        Command messageCommand = null;
        try {
            messageCommand = Arrays.stream(Command.values()).filter(command -> command.getRussian().toUpperCase().equals(message.toUpperCase().split(" ")[0])).findFirst().orElseThrow();
        }catch (Exception ignored){}
        if (user.getUserState() != BotState.NONE) {
            return busyReply(user);
        }else {
            assert messageCommand != null;
            if (messageCommand.equals(Command.CITY)) {
                return cityEnterReply(user);
            }else if (messageCommand.equals(Command.BACK)) {
                return backReply(user);
            }
        }return cityEnterReply(user);
    }



    private List<PartialBotApiMethod<? extends Serializable>> backReply(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Ты решил вернуться домой.");
        reply.setReplyMarkup(createBaseReplyKeyboard());
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> cityEnterReply(User user) {
        var buttons = new KeyboardButton[]{
                new KeyboardButton("⚒ Кузница"),
                new KeyboardButton("\uD83D\uDED2 Магазин"),
                new KeyboardButton("⬅️ Назад")};

        var reply = createMessageTemplate(user);
        reply.setText(String.format(
                "%nВ *кузнице* тебе смогут выковать редкое снаряжение." +
                        "%nВ *магазине* ты сможешь купить уже готовые предметы."));
        reply.setReplyMarkup(createKeyboard(buttons));

        return List.of(reply);
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.CITY, Command.BACK);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
