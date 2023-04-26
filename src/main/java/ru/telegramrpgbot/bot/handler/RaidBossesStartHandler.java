package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.RaidBoss;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.PartyRepository;
import ru.telegramrpgbot.repository.RaidBossRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.telegramrpgbot.bot.util.IngameUtil.userStaminaChanges;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createPhotoTemplate;

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

        try {
            raidBoss = raidBossRepository.findById(Long.parseLong(message.split("_")[1])).orElseThrow();
        }catch (Exception ignored){
            reply.setText("Проверьте правильность введенной команды.");
            return List.of(reply);
        }
        if (!partyMembers.stream().filter(member -> member.getCurrentStamina() < raidBoss.getStaminaRequired()).toList().isEmpty()){
            reply.setText("У кого-то из участников не хватает выносливости.");
            return List.of(reply);
        }
        partyMembers.remove(user);

        String imagePath = String.format("src/main/java/ru/telegramrpgbot/bot/images/Bosses/%s.png",raidBoss.getName());
        InputFile inputFile = null;
        try{
            inputFile = new InputFile(new File(imagePath));
        }catch (Exception ignored){}


        for (User member :
                partyMembers) {
            member.setUserState(BotState.RAIDING);
            userStaminaChanges(member,-raidBoss.getStaminaRequired());
            var anons = createPhotoTemplate(member);
            anons.setCaption(String.format("Капитан команды начал рейд на босса \uD83D\uDC7E %s.",raidBoss.getName()));
            anons.setPhoto(inputFile);
            replyList.add(anons);
            userRepository.save(member);
        }
        var image = createPhotoTemplate(user);
        image.setPhoto(inputFile);
        image.setCaption(String.format("Вы начали рейд на босса \uD83D\uDC7E %s.",raidBoss.getName()));
        user.setUserState(BotState.RAIDING);
        userStaminaChanges(user,-raidBoss.getStaminaRequired());
        userRepository.save(user);

        replyList.add(image);

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
            replyMessage.append(String.format("%n\uD83D\uDC7E *%s*%nРекомендованный уровень: %d\uD83D\uDCA0%nТребуется выносливости = %d⚡️ %nХарактеристики = %d♥️,  %d\uD83D\uDDE1,  %d \uD83D\uDEE1%n",
                    raidBoss.getName(),
                    raidBoss.getRecommendedLevel(),
                    raidBoss.getStaminaRequired(),
                    raidBoss.getLife(),
                    raidBoss.getDamage(),
                    raidBoss.getArmor()
            ));
            if (user.getHostPartyId() != null){
                replyMessage.append(String.format("Начать рейд - /raid\\_%d%n",raidBoss.getId()));
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
