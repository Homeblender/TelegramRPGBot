package ru.telegramrpgbot.bot.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegramrpgbot.bot.Bot;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.bot.util.TelegramUtil;
import ru.telegramrpgbot.model.Move;
import ru.telegramrpgbot.model.SoloActivityReward;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.MoveRepository;
import ru.telegramrpgbot.repository.SoloActivityRewardRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.IngameUtil.*;

@Component
@Slf4j
public class ScheduledCheckUsers {

    private final UserRepository userRepository;
    private final MoveRepository moveRepository;
    private final SoloActivityRewardRepository soloActivityRewardRepository;
    private final Bot bot;
    private final Random rnd = new Random();

    public ScheduledCheckUsers(UserRepository userRepository, SoloActivityRewardRepository soloActivityRewardRepository, MoveRepository moveRepository, Bot bot) {
        this.userRepository = userRepository;
        this.soloActivityRewardRepository = soloActivityRewardRepository;
        this.moveRepository = moveRepository;
        this.bot = bot;
    }


    @Scheduled(fixedDelay = 15000)
    public void CheckUsers() {
        List<User> afkUsers = userRepository.findAllByUserState(BotState.NONE);
        restorAskUsers(afkUsers);
        List<User> onSoloActivityUsers = userRepository.findAllByUserState(BotState.SOLO_ACTIVITY);
        checkSoloActivityEnds(onSoloActivityUsers);
        CheckMoves();
    }

    private void checkSoloActivityEnds(List<User> onSoloActivityUsers) {
        for (User user : onSoloActivityUsers) {
            if (user.getActivityEnds() != null && user.getActivityEnds().before(new Timestamp(System.currentTimeMillis()))) {

                log.info(user.getActivityEnds() + " " + user.getActivityId().getName());


                List<SoloActivityReward> possibleRewards = soloActivityRewardRepository.findAllBySoloActivity(user.getActivityId());
                var reward = possibleRewards.get(new Random().nextInt(possibleRewards.size()));

                var soloActivityEnded = TelegramUtil.createMessageTemplate(user);
                var gold = rnd.nextLong(reward.getGoldReward(), reward.getGoldReward() * 2);
                var exp =  rnd.nextLong(reward.getExpReward(), reward.getExpReward() * 2);

                var reply = reward.getResultMessage() + String.format("%n%nНаграда:%n+%d зол. монет%n+%d очк. опыта",gold, exp );
                if (reward.getItemReward() != null) {
                    reply += String.format("%n+%s", reward.getItemReward().getName());
                    userGetItem(user, reward.getItemReward());
                }

                userGoldChanges(user, gold);
                userExpChanged(user, exp);
                user.setUserState(BotState.NONE);
                user.setActivityEnds(null);
                user.setActivityId(null);
                userRepository.save(user);

                soloActivityEnded.setText(reply);
                executeWithExceptionCheck(soloActivityEnded);
            }
        }
    }

    private void executeWithExceptionCheck(SendMessage sendMessage) {
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void restorAskUsers(List<User> afkUsers) {

        for (User user : afkUsers) {
            if (!user.getCurrentHealth().equals(user.getMaxHealth())) userHealthChanges(user, 1L);

            if (!user.getCurrentMana().equals(user.getMaxMana())) userManaChanges(user, 1L);

            if (!user.getCurrentStamina().equals(user.getMaxStamina())) {
                if (user.getStaminaRestor().before(new Timestamp(System.currentTimeMillis()))) {
                    userStaminaChanges(user, 1L);
                    if (user.getCurrentStamina().equals(user.getMaxStamina())) {
                        var staminaRestoredMessage = TelegramUtil.createMessageTemplate(user);
                        user.setStaminaRestor(null);
                        staminaRestoredMessage.setText("*Ваша выносливность полностью остановлена!*");
                        executeWithExceptionCheck(staminaRestoredMessage);
                    } else {
                        var delay = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
                        user.setStaminaRestor(new Timestamp(System.currentTimeMillis() + delay));
                    }
                    userRepository.save(user);

                }
            }
        }
    }
    public void CheckMoves() {
        List<Move> moves = moveRepository.findAll();
        for (Move move : moves) {
            if (move.getEndTime().before(new Timestamp(System.currentTimeMillis()))) {
                var messagesToSend = IngameUtil.cancel(move.getUserId());
                if (messagesToSend != null && !messagesToSend.isEmpty()) {
                    messagesToSend.forEach(response -> {
                        if (response instanceof SendMessage) {
                            executeWithExceptionCheck((SendMessage) response);
                        }
                    });
                }
            }
        }
    }

}
