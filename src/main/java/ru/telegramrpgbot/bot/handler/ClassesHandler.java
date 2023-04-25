package ru.telegramrpgbot.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
public class ClassesHandler implements Handler {
    private final UserRepository userRepository;

    public ClassesHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).split("_")[0].toUpperCase().equals(Command.CLASS.name())) {
            return classAccept(user, message);
        }
        return showClasses(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> classAccept(User user, String message) {
        var reply = createMessageTemplate(user);
        var availableClasses = IngameUtil.getAvailableClasses(user);
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText(createClassMessage(user.getUserClass()));
            return List.of(reply);
        }
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты.");
            return List.of(reply);
        }
        Class classToChange;
        try {
            classToChange = availableClasses.stream().filter(userClass-> userClass.getId() ==Long.parseLong(messageList.get(1))).findAny().orElse(null);
        } catch (NumberFormatException exception) {
            reply.setText("Id класса должно быть числом.");
            return List.of(reply);
        }
        if (classToChange == null){
            reply.setText("Вы не можете выбрать этот класс, либо такого класса не существует.");
            return List.of(reply);
        }

        user.setUserClass(classToChange);
        userRepository.save(user);
        reply.setText(String.format("Вы успешно выбрали класс *%s*.%n",classToChange.getName()));
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showClasses(User user) {
        var reply = createMessageTemplate(user);
        var availableClasses = IngameUtil.getAvailableClasses(user);
        if (availableClasses.isEmpty()) {
            reply.setText("Нет доступных классов.");
            return List.of(reply);
        }

        StringBuilder replyMessage = new StringBuilder(String.format("Доступные классы:%n%n"));

        for (Class userClass :
                availableClasses) {
            replyMessage.append(createClassMessage(userClass));
            replyMessage.append(createClassAcceptMessage(userClass));
        }

        reply.setText(replyMessage.toString());
        return List.of(reply);
    }

    private String createClassMessage(Class userClass) {
        return String.format("*%s* - %s%n", userClass.getName(), userClass.getDescription());
    }

    private Object createClassAcceptMessage(Class userClass) {
        return String.format("Выбрать класс - /class\\_%d%n%n",userClass.getId());
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.CLASSES, Command.CLASS);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
