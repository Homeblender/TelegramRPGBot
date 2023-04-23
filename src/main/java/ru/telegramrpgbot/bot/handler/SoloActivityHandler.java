package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.SoloActivity;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.SoloActivityRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.IngameUtil.userStaminaChanges;
import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class SoloActivityHandler implements Handler {
    private final UserRepository userRepository;
    private final SoloActivityRepository soloActivityRepository;
    private final List<SoloActivity> allActivities;
    private final List<String> AVAILABLE_ACTIVITIES = new ArrayList<>();

    public SoloActivityHandler(UserRepository userRepository, SoloActivityRepository soloActivityRepository) {
        this.userRepository = userRepository;
        this.soloActivityRepository = soloActivityRepository;
        allActivities = soloActivityRepository.findAll(Sort.by("requiredLevel").ascending());
        for (SoloActivity item :
                allActivities) {
            AVAILABLE_ACTIVITIES.add(item.getName());
        }
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (user.getUserState() != BotState.NONE) {
            return busyReply(user);
        } else if (AVAILABLE_ACTIVITIES.contains(message)) {
            return starActivity(user, message);
        }
        return showActivities(user);
    }



    private List<PartialBotApiMethod<? extends Serializable>> noStaminaReply(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("У тебя недостаточно выносливости.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> starActivity(User user, String message) {
        SoloActivity activity = soloActivityRepository.getSoloActivityByName(message);


        if (user.getCurrentStamina() < activity.getRequiredStamina()) {
            return noStaminaReply(user);
        }

        user.setActivityId(activity);
        user.setUserState(BotState.SOLO_ACTIVITY);
        var delay = TimeUnit.MILLISECONDS.convert(activity.getActivityDuration(), TimeUnit.MINUTES);
        user.setActivityEnds(new Timestamp(System.currentTimeMillis() + delay));
        userStaminaChanges(user, -activity.getRequiredStamina());
        userRepository.save(user);

        var reply = createMessageTemplate(user);
        reply.setText(String.format("Ты отправился в *%s*.%n%nВремя до возвращения - *%d* мин.", activity.getName(), activity.getActivityDuration()));

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showActivities(User user) {
        List<SoloActivity> availableActivities = allActivities.stream().filter(w -> w.getRequiredLevel() <= user.getLevel()).toList();
        var reply = createMessageTemplate(user);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        StringBuilder replyText = new StringBuilder("Выбери понравившееся приключение из доступных тебе:\n");

        for (SoloActivity soloActivity : availableActivities) {
            inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton(soloActivity.getName().split(" ")[0], soloActivity.getName()));
            replyText.append(String.format("\n*%s* (%d мин.) (%d ⚡️):\n\n%s%n", soloActivity.getName(),soloActivity.getRequiredStamina(), soloActivity.getActivityDuration(), soloActivity.getDescription()));
        }
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setText(replyText.toString());
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return List.of(reply);
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.ADVENTURES);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return AVAILABLE_ACTIVITIES;
    }
}
