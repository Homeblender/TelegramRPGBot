package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.*;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.repository.AppliedSkillRepository;
import ru.telegramrpgbot.repository.SkillRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class PassivesHandler implements Handler {

    private final SkillRepository skillRepository;
    private final AppliedSkillRepository appliedSkillRepository;
    private final UserRepository userRepository;

    public PassivesHandler(SkillRepository skillRepository, AppliedSkillRepository appliedSkillRepository, UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.appliedSkillRepository = appliedSkillRepository;
        this.userRepository = userRepository;
    }
    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.toUpperCase().contains(Command.APPLY.name())) {
            return applySkill(user, message);
        }else if (message.substring(1).toUpperCase().equals(Command.RELEARN.name())) {
            return relearnSkills(user);
        }
        return showPassives(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> relearnSkills(User user) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        var skills = appliedSkillRepository.findAllByUser(user);
        for (AppliedSkill skill :
                skills) {
            user.setMaxMana(user.getMaxMana()-skill.getSkill().getManaBonus() * skill.getSkillLevel());
            user.setMaxHealth(user.getMaxHealth()-skill.getSkill().getHealthBonus() * skill.getSkillLevel());
        }
        user.setPassivePoints(user.getLevel()-1);
        appliedSkillRepository.deleteAll(skills);
        userRepository.save(user);
        reply.setText("Вы отменили все изученные пассивные умения.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> applySkill(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        Skill skill;
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }

        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id пассивного умения.");
            return List.of();
        }

        if (user.getPassivePoints()<1) {
            reply.setText("У вас не достаточно очков пассивных умений.");
            return List.of();
        }
        try {
            skill = skillRepository.findById(Long.parseLong(messageList.get(1))).orElseThrow();
        } catch (Exception exception) {
            reply.setText("Такого пассивного умения не существует.");
            return List.of(reply);
        }
        List<Class> availableClasses = new ArrayList<>();
        for (Class userClass = user.getUserClass(); userClass != null; userClass = userClass.getBaseClass()) {
            availableClasses.add(userClass);
        }
        if (skillRepository.findAllByClassIdIn(availableClasses).stream().noneMatch(w-> w.equals(skill))) {
            reply.setText("Вы не можете изучить это пассивное умение.");
            return List.of(reply);
        }
        AppliedSkill appliedSkill = appliedSkillRepository.findAllBySkillAndUser(skill,user).orElseGet(() ->
                appliedSkillRepository.save(AppliedSkill.builder()
                        .user(user).skillLevel(0L).skill(skill)
                        .build()));

        appliedSkill.setSkillLevel(appliedSkill.getSkillLevel()+1);
        appliedSkillRepository.save(appliedSkill);

        user.setMaxHealth(user.getMaxHealth()+skill.getHealthBonus());
        user.setMaxMana(user.getMaxMana()+skill.getManaBonus());
        user.setPassivePoints(user.getPassivePoints()-1);
        userRepository.save(user);
        reply.setText(String.format("Вы успешно изучили *%s*",skill.getName()));
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showPassives(User user) {
        var reply = createMessageTemplate(user);

        List<Class> availableClasses = new ArrayList<>();
        for (Class userClass = user.getUserClass(); userClass != null; userClass = userClass.getBaseClass()) {
            availableClasses.add(userClass);
        }
        StringBuilder replyMessage = new StringBuilder(String.format("Все пассивные умения доступные вам.%n%n"));


        for (Class userClass :
                availableClasses) {
            var skills = skillRepository.findAllByClassId(userClass);
            replyMessage.append(String.format("Класс *%s*:%n%n",userClass.getName()));

            for (Skill skill :
                    skills) {
                var applied = appliedSkillRepository.findAllBySkillAndUser(skill,user).orElse(null);

                replyMessage.append(String.format("\\[%d] *%s*    %s%s%s%s%nПрокачать - /apply\\_%d%n%n",
                        applied == null?0 : applied.getSkillLevel(),
                        skill.getName(),
                        skill.getDamageBonus() == 0? "" : String.format("+%d \uD83D\uDDE1 ",skill.getDamageBonus()),
                        skill.getArmorBonus() == 0? "" : String.format("+%d \uD83D\uDEE1 ",skill.getArmorBonus()),
                        skill.getHealthBonus() == 0? "" : String.format("+%d ♥️ ",skill.getHealthBonus()),
                        skill.getManaBonus() == 0? "" : String.format("+%d \uD83D\uDD39 ",skill.getManaBonus()),
                        skill.getId()));
            }
        }
        replyMessage.append("Отменить все изученные пассивные умения - /relearn");
        reply.setText(replyMessage.toString());
        return List.of(reply);

    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.PASSIVES,Command.APPLY,Command.RELEARN);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
