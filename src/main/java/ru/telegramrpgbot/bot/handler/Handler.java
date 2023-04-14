package ru.telegramrpgbot.bot.handler;

import ru.telegramrpgbot.enums.BotState;
import ru.telegramrpgbot.enums.Command;
import ru.telegramrpgbot.model.User;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.io.Serializable;
import java.util.List;

public interface Handler {
    List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message);
    BotState operatedBotState();
    Command operatedCommand();
    List<String> operatedCallBackQuery();
}
