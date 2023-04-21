package ru.telegramrpgbot.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
public class StartCommandHandler implements Handler{
    private final UserRepository userRepository;

    public StartCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>>handle(User user, String message) {
        SendMessage welcomeMessage = createMessageTemplate(user);
        welcomeMessage.setText("Привет!");
        SendMessage registrationMessage = createMessageTemplate(user);
        registrationMessage.setText("Назови себя.");
        user.setUserState(BotState.WAITING_FOR_NAME);
        userRepository.save(user);

        return List.of(welcomeMessage, registrationMessage);
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.START);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of();
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
