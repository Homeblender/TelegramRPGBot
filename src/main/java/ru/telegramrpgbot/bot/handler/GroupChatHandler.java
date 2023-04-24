package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.util.TelegramUtil;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class GroupChatHandler implements Handler {

    private final UserRepository userRepository;

    public GroupChatHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message == null) {
            return baseGroupMessage(user);
        }else if (message.toUpperCase().substring(1).split("_")[0].equals(Command.GRANT.name())) {
            return grandRules(user,message);
        }
        return List.of();
    }

    private List<PartialBotApiMethod<? extends Serializable>> grandRules(User user, String message) {
        var reply = createMessageTemplate(user);

        List<String> messageList = Arrays.stream(message.split("_")).toList();
        reply.setReplyMarkup(createBaseReplyKeyboard());

        User grantee;

        if (messageList.size() != 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указано имя получателя прав.");
            return List.of(reply);
        }
        if (!user.getIsGameMaster()) {
            reply.setText("Вы не администратор. \uD83D\uDE1F");
            return List.of(reply);
        }
        try {
            grantee = userRepository.findAllByName(messageList.get(1)).orElseThrow();
        } catch (NumberFormatException exception) {
            reply.setText("Нет такого игрока.");
            return List.of(reply);
        }
        if (grantee.getIsGameMaster()) {
            reply.setText("Этот игрок уже администратор");
            return List.of(reply);
        }
        var granteeMessage = createMessageTemplate(grantee);
        granteeMessage.setText(String.format("Игрок _%s_ выдал вам права создавать *события* от имени администратора.\uD83D\uDE0E",user.getName()));
        grantee.setIsGameMaster(true);
        userRepository.save(grantee);
        reply.setText(String.format("Вы успешно выдали *права администратора* игроку _%s_.",grantee.getName()));
        return List.of(granteeMessage,reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> baseGroupMessage(User user) {
        var reply = TelegramUtil.createMessageTemplate(user);
        user.setIsGameMaster(true);
        userRepository.save(user);
        reply.setText(String.format("Вы добавили меня в группу!%n%nТеперь вы можете создавать *события* от имени администратора. \uD83D\uDE0E%n%nЧтобы выдать права администратора другому игроку напишите команду - '/grant\\_[имя].'"));
        return List.of(reply);
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.GRANT);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
