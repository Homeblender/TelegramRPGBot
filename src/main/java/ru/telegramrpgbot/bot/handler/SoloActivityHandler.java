package ru.telegramrpgbot.bot.handler;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
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

    private List<PartialBotApiMethod<? extends Serializable>> busyReply(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Сейчас ты занят другим.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> noStaminaReply(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("У тебя недостаточно выносливости.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> starActivity(User user, String message) {
        SoloActivity activity = soloActivityRepository.getSoloActivityByName(message);


        if (user.getCurrentStamina() == 0) {
            return noStaminaReply(user);
        }

        user.setActivityId(activity);
        user.setUserState(BotState.SOLO_ACTIVITY);
        var delay = TimeUnit.MILLISECONDS.convert(activity.getActivityDuration(), TimeUnit.MINUTES);
        user.setActivityEnds(new Timestamp(System.currentTimeMillis() + delay));
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

        for (SoloActivity item : availableActivities) {
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
