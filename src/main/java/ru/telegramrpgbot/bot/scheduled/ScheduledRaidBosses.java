package ru.telegramrpgbot.bot.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegramrpgbot.bot.Bot;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.*;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static ru.telegramrpgbot.bot.util.IngameUtil.*;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class ScheduledRaidBosses {

    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final ClassRepository classRepository;
    private final GroupChatRepository groupChatRepository;
    private final RaidBossItemRewardRepository raidBossItemRewardRepository;
    private final Bot bot;
    private final Random rnd = new Random();

    public ScheduledRaidBosses(UserRepository userRepository, PartyRepository partyRepository, ClassRepository classRepository, GroupChatRepository groupChatRepository, RaidBossItemRewardRepository raidBossItemRewardRepository, Bot bot) {
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
        this.classRepository = classRepository;
        this.groupChatRepository = groupChatRepository;
        this.raidBossItemRewardRepository = raidBossItemRewardRepository;
        this.bot = bot;
    }


    @Scheduled(fixedDelay = 5000)
    public void CheckParties() {
        var parties = partyRepository.findAllByBossFightingNotNull();

        for (Party party : parties) {
            partyFight(party);
        }

    }

    private void partyFight(Party party) {
        var message = new StringBuilder();
        List<SendMessage> sendMessageList = new ArrayList<>();


        if (party.getBossFighting() == null || party.getBossLife() == null) {
            party.setBossLife(null);
            party.setBossFighting(null);
            return;
        }
        //Юзеры которые дерутся
        List<User> allUsers = userRepository.findAllByPartyId(party);

        //Юзеры которые еще живы
        List<User> activeUsers = allUsers.stream().filter(user -> user.getCurrentHealth() > 0).toList();

        RaidBoss boss = party.getBossFighting();
        if (activeUsers.isEmpty()) {
            sendAllMessages(Objects.requireNonNull(partyDead(party)));
            return;
        }
        Class warrior = classRepository.findByName("⚔ Воин").orElseThrow();
        List<Class> classes = getAvailableClasses(warrior);

        //Юзеры с классом воин либо его подклассами
        List<User> tankUsers = activeUsers.stream().filter(user -> classes.contains(user.getUserClass())).toList();
        tankUsers = tankUsers.isEmpty() ? activeUsers : tankUsers;

        //Юзер который получает урон
        User takesDamage = tankUsers.get((rnd.nextInt(tankUsers.size())));
        //Урон по юзеру
        var damageToUser = damageTakes(countArmor(takesDamage), boss.getDamage());

        message.append(String.format("Сражение группы _%s_ с *боссом* \uD83D\uDC7E %s%n%n", party.getName(), boss.getName()));

        message.append(String.format("\uD83D\uDC7E*%s* атакует.%n" +
                        "Игрок _%s_ теряет %d♥️%n%n",
                boss.getName(),
                takesDamage.getName(),
                damageToUser
        ));
        //Юзер получает урон
        userHealthChanges(takesDamage, -damageToUser);

        //Урон по боссу

        long sumAttack = 0;

        for (User user :
                activeUsers) {
            sumAttack += countDamage(user);
        }
        var damageToBoss = damageTakes(boss.getArmor(), sumAttack);

        message.append(String.format("Команда _%s_ атакует.%n" +
                        "\uD83D\uDC7E*%s* теряет %d♥️%n%n",
                party.getName(),
                boss.getName(),
                damageToBoss
        ));
        party.setBossLife(party.getBossLife() - damageToBoss);
        partyRepository.save(party);


        message.append(String.format("Здровье ♥️ босса - %s%n", party.getBossLife() < 0 ? 0 : party.getBossLife()));

        for (User user :
                allUsers) {
            var messageTMP = createMessageTemplate(user);
            var messageText = message + String.format("Ваше здоровье ♥️ - %d", user.getCurrentHealth());
            messageTMP.setReplyMarkup(createBaseReplyKeyboard());
            messageTMP.setText(messageText);
            sendMessageList.add(messageTMP);
        }
        if (party.getBossLife() <= 0) {
            sendMessageList.addAll(Objects.requireNonNull(partyWins(party)));
        }
        sendAllMessages(sendMessageList);
    }

    private List<SendMessage> partyWins(Party party) {
        List<User> allUsers = userRepository.findAllByPartyId(party);
        List<GroupChat> allChats = groupChatRepository.findAll();

        List<SendMessage> sendMessageList = new ArrayList<>();

        for (User user :
                allUsers) {
            var messageTMP = createMessageTemplate(user);
            messageTMP.setReplyMarkup(createBaseReplyKeyboard());

            var gold = rnd.nextLong(party.getBossFighting().getGoldReward(), party.getBossFighting().getGoldReward() * 2);
            var exp = rnd.nextLong(party.getBossFighting().getExpReward(), party.getBossFighting().getExpReward() * 2);
            messageTMP.setText(String.format("Ваша команда победила босса \uD83D\uDC7E%s.", party.getBossFighting().getName()) + String.format("%n%nНаграда:%n+%d \uD83D\uDCB0 зол. монет%n+%d \uD83C\uDF1F очк. опыта%n%n", gold, exp));
            var reward = reward(user, party.getBossFighting());
            for (BaseItem baseItem :
                    reward) {
                messageTMP.setText(messageTMP.getText() + String.format("+ _%s_ x1%n", baseItem.getName()));
            }
            userGoldChanges(user, gold);
            userExpChanged(user, exp);
            user.setUserState(BotState.NONE);
            userRepository.save(user);
            sendMessageList.add(messageTMP);
        }
        StringBuilder anons = new StringBuilder(String.format("Команда _%s_ победила в сражении с боссом \uD83D\uDC7E%s.%n%n", party.getName(), party.getBossFighting().getName()));
        return getSendMessages(party, allChats, sendMessageList, anons);
    }

    private List<SendMessage> getSendMessages(Party party, List<GroupChat> allChats, List<SendMessage> sendMessageList, StringBuilder anons) {
        List<User> partyUsers = userRepository.findAllByPartyId(party);
        for (User partyUser : partyUsers) {
            anons.append(String.format("[%s](tg://user?id=%d) - Уровень -\uD83D\uDCA0%d (%d \uD83D\uDDE1, %d \uD83D\uDEE1) %n",
                    partyUser.getName(),
                    partyUser.getChatId(),
                    partyUser.getLevel(),
                    IngameUtil.countDamage(partyUser),
                    IngameUtil.countArmor(partyUser)));
        }

        for (GroupChat chat :
                allChats) {
            var messageTMP = createMessageTemplate(chat.getId().toString());
            messageTMP.setText(anons.toString());
            sendMessageList.add(messageTMP);
        }
        party.setBossFighting(null);
        party.setBossLife(null);
        partyRepository.save(party);
        return sendMessageList;
    }

    private List<BaseItem> reward(User user, RaidBoss raidBoss) {
        var allItems = raidBossItemRewardRepository.findByBoss(raidBoss).stream().map(RaidBossItemReward::getItem).toList();
        List<BaseItem> reward = new ArrayList<>();
        int itemsCount = rnd.nextInt(1, 3);
        for (int i = 0; i < itemsCount; i++) {
            reward.add(allItems.get(rnd.nextInt(allItems.size())));
            userGetItem(user,reward.get(i));
        }
        return reward;
    }

    private List<SendMessage> partyDead(Party party) {
        List<User> allUsers = userRepository.findAllByPartyId(party);
        List<GroupChat> allChats = groupChatRepository.findAll();

        List<SendMessage> sendMessageList = new ArrayList<>();

        for (User user :
                allUsers) {
            var messageTMP = createMessageTemplate(user);
            messageTMP.setReplyMarkup(createBaseReplyKeyboard());
            messageTMP.setText(String.format("Ваша команда проиграла сражение с боссом \uD83D\uDC7E%s.", party.getBossFighting().getName()));
            user.setUserState(BotState.NONE);
            userRepository.save(user);
            sendMessageList.add(messageTMP);
        }
        StringBuilder anons = new StringBuilder(String.format("Команда _%s_ проиграла сражение с боссом \uD83D\uDC7E%s.%n%n", party.getName(), party.getBossFighting().getName()));
        return getSendMessages(party, allChats, sendMessageList, anons);
    }

    private void sendAllMessages(List<SendMessage> sendMessageList) {
        for (SendMessage sendMessage : sendMessageList) {
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException ignored) {
            }
        }
    }
}
