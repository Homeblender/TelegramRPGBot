package ru.telegramrpgbot.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
public class StartHandler implements Handler{
    private final UserRepository userRepository;

    public StartHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>>handle(User user, String message) {
        SendMessage welcomeMessage = createMessageTemplate(user);
        welcomeMessage.setText("Привет, странник! Давай, я познакомлю тебя с моим миром.\n\n" +
                "В недалеком будущем случилась глобальная мистическая катастрофа. Произошло столкновение миров, и в наш мир вторглись фэнтезийные монстры и принесли с собой магию. Люди пытались сопротивляться, но все было тщетно. Большинство населения погибло. Выжившие потеряли все технологии, а спустя несколько поколений и память о былом мире. Люди стали враждовать за рессурсы. Жизнь человечества откатилась до уровня средневековья.\n" +
                "\nНо все изменилось, когда люди смогли овладеть магией. Теперь в сердцах людей снова теплится надежда на мир, свободный от страшных неземных чудищ.\n" +
                "\nВам предстоит исследовать новый чудный мир, путешествуя по развалинам древней цивилизации, а также овладеть новыми навыками и увеличить свою силу, чтобы помочь человечеству в борьбе с монстрами. Но будьте осторожны, ведь напасть на вас могут не только монстры, но и другие люди.\n");
        SendMessage registrationMessage = createMessageTemplate(user);
        registrationMessage.setText("Назови свое имя");
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
