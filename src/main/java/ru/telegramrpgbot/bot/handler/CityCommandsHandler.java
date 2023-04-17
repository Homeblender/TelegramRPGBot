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

        var message_real = Arrays.stream(Command.values()).filter(command -> command.getRussian().toUpperCase().equals(message.toUpperCase().split(" ")[1])).findFirst().get();
        if (user.getUserState() != BotState.NONE) {
            return busyReply(user);
        } else if (message_real.equals(Command.CITY)) {
            return cityEnterMessage(user);
        }else if (message_real.equals(Command.BACK)) {
            return backCityMessage(user);
        }
        return null;
    }

    private List<PartialBotApiMethod<? extends Serializable>> backCityMessage(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Ты решил вернуться домой.");
        reply.setReplyMarkup(createBaseReplyKeyboard());
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> cityEnterMessage(User user) {
        var buttons = new KeyboardButton[] {
                new KeyboardButton("⚒ Кузница"),
                new KeyboardButton("\uD83D\uDED2 Магазин"),
                new KeyboardButton("⬅️ Назад")};

        var reply = createMessageTemplate(user);
        reply.setText(String.format("Ты пришел в *город*." +
                "%n*Кузнец* может выковать тебе подходящее снаряжение." +
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
        return List.of(Command.CITY, Command.FORGE, Command.SHOP, Command.BACK);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
