package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.RaidBoss;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.PartyRepository;
import ru.telegramrpgbot.repository.RaidBossRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class RaidBossesStartHandler implements Handler {

    private final RaidBossRepository raidBossRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;

    public RaidBossesStartHandler(RaidBossRepository raidBossRepository, UserRepository userRepository, PartyRepository partyRepository) {
        this.raidBossRepository = raidBossRepository;
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).toUpperCase().split("_")[0].equals("RAID")){
            return startRaid(user, message);
        }
        return showRaidBosses(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> startRaid(User user, String message) {
        var reply = createMessageTemplate(user);
        List<PartialBotApiMethod<? extends Serializable>> replyList = new ArrayList<>();
        RaidBoss raidBoss;
        if (user.getHostPartyId() == null){
            reply.setText("Только лидер группы может начать рейд.");
            return List.of(reply);
        }
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        var partyMembers = userRepository.findAllByPartyId(user.getPartyId());
        if (!partyMembers.stream().filter(member -> member.getUserState() != BotState.NONE).toList().isEmpty()){
            reply.setText("Кто-то из участников группы занят.");
            return List.of(reply);
        }
        partyMembers.remove(user);
        try {
            raidBoss = raidBossRepository.findById(Long.parseLong(message.split("_")[1])).orElseThrow();
        }catch (Exception ignored){
            reply.setText("Проверьте правильность введенной команды.");
            return List.of(reply);
        }

        for (User member :
                partyMembers) {
            member.setUserState(BotState.RAIDING);
            var anons = createMessageTemplate(member);
            anons.setText(String.format("Капитан команды начал рейд на босса \uD83D\uDC7E*%s*.",raidBoss.getName()));
            replyList.add(anons);
            userRepository.save(member);
        }
        reply.setText(String.format("Вы начали рейд на босса \uD83D\uDC7E*%s*.",raidBoss.getName()));
        user.setUserState(BotState.RAIDING);
        userRepository.save(user);

        replyList.add(reply);

        var party = user.getPartyId();
        party.setBossFighting(raidBoss);
        party.setBossLife(raidBoss.getLife());
        partyRepository.save(party);
        return replyList;
    }

    private List<PartialBotApiMethod<? extends Serializable>> showRaidBosses(User user) {
        var reply = createMessageTemplate(user);
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        if (user.getPartyId() == null) {
            reply.setText("Для сражения с рейдовыми боссами необходима *группа*.\nСоздайте ее либо вступите в уже существующую.");
            return List.of(reply);
        }
        var bosses = raidBossRepository.findAll();

        StringBuilder replyMessage = new StringBuilder(String.format("Рейдовые *боссы*:%n"));

        for (RaidBoss raidBoss : bosses) {
            replyMessage.append(String.format("%n\uD83D\uDC7E *%s*    Рекомендованный уровень = %d %nХарактеристики - %d♥️,  %d\uD83D\uDDE1,  %d \uD83D\uDEE1%n",
                    raidBoss.getName(),
                    raidBoss.getRecommendedLevel(),
                    raidBoss.getLife(),
                    raidBoss.getDamage(),
                    raidBoss.getArmor()
            ));
            if (user.getHostPartyId() != null){
                replyMessage.append(String.format("Атаковать - /raid\\_%d%n",raidBoss.getId()));
            }
        }

        replyMessage.append("\nДополнительная информация -  /guide\\_raid");
        reply.setText(replyMessage.toString());
        return List.of(reply);
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.BOSSES, Command.RAID);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
