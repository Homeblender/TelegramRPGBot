package ru.telegramrpgbot.bot.handler;

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

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
public class StartCommandHandler implements Handler{
    private final UserRepository userRepository;

    public StartCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>>handle(User user, String message) {
        // Приветствуем пользователя
        SendMessage welcomeMessage = createMessageTemplate(user);
        welcomeMessage.setText("Привет!");
        // Просим назваться
        SendMessage registrationMessage = createMessageTemplate(user);
        registrationMessage.setText("Назови себя.");
        // Меняем пользователю статус на - "ожидание ввода имени"
        user.setUserState(BotState.WAITING_FOR_NAME);
        userRepository.save(user);

        return List.of(welcomeMessage, registrationMessage);
    }

    @Override
    public BotState operatedBotState() {
        return BotState.START;
    }

    @Override
    public Command operatedCommand() {
        return null;
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return Collections.emptyList();
    }
}
