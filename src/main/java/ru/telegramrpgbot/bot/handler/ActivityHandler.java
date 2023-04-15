package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.enums.BotState;
import ru.telegramrpgbot.enums.Command;
import ru.telegramrpgbot.model.SoloActivity;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.SoloActivityRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class ActivityHandler implements Handler{
    private final UserRepository userRepository;
    private final SoloActivityRepository soloActivityRepository;
    private final List<SoloActivity> allActivities;
    private final List<String> AVAILABLE_ACTIVITIES = new ArrayList<>();

    public ActivityHandler(UserRepository userRepository, SoloActivityRepository soloActivityRepository) {
        this.userRepository = userRepository;
        this.soloActivityRepository =  soloActivityRepository;
        allActivities = soloActivityRepository.findAll(Sort.by("requiredLevel").ascending());
        for (SoloActivity item :
                allActivities) {
            AVAILABLE_ACTIVITIES.add(item.getName());
        }
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>>handle(User user, String message) {
        if (AVAILABLE_ACTIVITIES.contains(message) && user.getCurrentUserState() == BotState.NONE) {
            return starActivity(user, message);
        }
        return showActivities(user, message);
    }

    private List<PartialBotApiMethod<? extends Serializable>> starActivity(User user, String message) {
        SoloActivity activity = soloActivityRepository.getSoloActivityByName(message);
        if (activity == null) throw new NullPointerException("oops");

        user.setActivityId(activity);

        var delay = TimeUnit.MILLISECONDS.convert(activity.getActivityDuration(), TimeUnit.MINUTES);
        user.setActivityEnds(new Timestamp(System.currentTimeMillis()+delay));

        userRepository.save(user);

        var reply = createMessageTemplate(user);
        reply.setText(String.format("Ты отправился в *%s*.%nВремя до возвращения - *%d* мин.",activity.getName(),activity.getActivityDuration()));

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showActivities(User user, String message) {
        List<SoloActivity> availableActivities = allActivities.stream().filter(w -> w.getRequiredLevel() <= user.getLevel()).toList();

        var reply = createMessageTemplate(user);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        StringBuilder replyText = new StringBuilder("Выбери понравившееся приключение из доступных тебе:\n");

        for (SoloActivity item: availableActivities) {
            inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton(item.getName(), item.getName()));
            replyText.append(String.format("*%s*:\n%s", item.getName(), item.getDescription()));
        }
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setText(replyText.toString());
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return List.of(reply);
    }


    @Override
    public BotState operatedBotState() {
        return null;
    }

    @Override
    public Command operatedCommand() {
        return Command.Activity;
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return AVAILABLE_ACTIVITIES;
    }
}
