package ru.telegramrpgbot.bot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegramrpgbot.bot.Bot;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.model.*;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.repository.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class IngameUtil {
    private static UserRepository userRepository;
    private static IngameItemRepository ingameItemRepository;
    private static AppliedSkillRepository appliedSkillRepository;
    private static ClassRepository classRepository;
    private static MoveRepository moveRepository;
    private static FightRepository fightRepository;
    private static GroupChatRepository groupChatRepository;
    private static Bot bot;

    public IngameUtil(UserRepository userRepository, IngameItemRepository ingameItemRepository, Bot bot, ClassRepository classRepository, MoveRepository moveRepository, FightRepository fightRepository, AppliedSkillRepository appliedSkillRepository, GroupChatRepository groupChatRepository) {
        IngameUtil.userRepository = userRepository;
        IngameUtil.ingameItemRepository = ingameItemRepository;
        IngameUtil.classRepository = classRepository;
        IngameUtil.moveRepository = moveRepository;
        IngameUtil.fightRepository = fightRepository;
        IngameUtil.appliedSkillRepository = appliedSkillRepository;
        IngameUtil.groupChatRepository = groupChatRepository;
        IngameUtil.bot = bot;
    }

    public static Long userHealthChanges(User user, Long healthChanged) {
        user.setCurrentHealth(user.getCurrentHealth() + healthChanged);
        if (user.getCurrentHealth() > user.getMaxHealth()) user.setMaxHealth(user.getCurrentHealth());
        else if (user.getCurrentHealth() < 0) {
            user.setCurrentHealth(0L);
            userDied(user);
        }
        userRepository.save(user);
        return user.getCurrentHealth();
    }

    public static void userManaChanges(User user, Long manaChanged) {
        user.setCurrentMana(user.getCurrentMana() + manaChanged);
        if (user.getCurrentMana() > user.getMaxMana()) user.setMaxMana(user.getCurrentMana());
        else if (user.getCurrentMana() < 0) {
            user.setCurrentMana(0L);
            userDied(user);
        }
        userRepository.save(user);
    }

    public static void userStaminaChanges(User user, Long staminaChanged) {
        user.setCurrentStamina(user.getCurrentStamina() + staminaChanged);
        if (user.getCurrentStamina() > user.getMaxStamina()) {
            user.setCurrentStamina(user.getMaxStamina());
        }if (user.getCurrentStamina() < user.getMaxStamina()) {
            var delay = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
            user.setStaminaRestor(new Timestamp(System.currentTimeMillis() + delay));
        }
        if (user.getMaxStamina().equals(user.getCurrentStamina())) {
            user.setStaminaRestor(null);
        }
        userRepository.save(user);
    }

    public static void userGoldChanges(User user, Long goldChanged) {
        user.setGold(user.getGold() + goldChanged);
        userRepository.save(user);
    }

    public static void userExpChanged(User user, Long expChanged) {
        user.setExp(user.getExp() + expChanged);
        userRepository.save(user);
        levelUp(user);
    }

    public static void levelUp(User user) {
        if (user.getExp() >= countExpToLevel(user.getLevel() + 1)) {
            user.setExp(user.getExp() - countExpToLevel(user.getLevel() + 1));

            var levelUpMessage = TelegramUtil.createMessageTemplate(user);
            levelUpMessage.setText("Вы достигли нового уровня и получили одно очко пассивных умений /passives");
            try {
                bot.execute(levelUpMessage);
            } catch (TelegramApiException e) {
                log.info(e.getMessage());
            }
            user.setLevel(user.getLevel() + 1);
            user.setCurrentStamina(user.getMaxStamina());
            user.setStaminaRestor(null);
            user.setCurrentHealth(user.getMaxHealth());
            user.setCurrentMana(user.getMaxMana());
            user.setPassivePoints(user.getPassivePoints() + 1);
            userRepository.save(user);
            levelUp(user);
        }

    }

    public static List<PartialBotApiMethod<? extends Serializable>> Announcement(String message) {
        var chats = groupChatRepository.findAll();
        List<PartialBotApiMethod<? extends Serializable>> sendMessageList = new ArrayList<>();
        for (GroupChat groupChat : chats) {
            var messageTemplate = createMessageTemplate(groupChat.getId().toString());
            messageTemplate.setText(message);
            sendMessageList.add(messageTemplate);
        }
        return sendMessageList;
    }

    public static void userGetItem(User user, BaseItem baseItem) {
        var ingameItems = ingameItemRepository.findAllByUser(user);
        ingameItems = ingameItems.stream()
                .filter(item ->
                        item.getBaseItem().equals(baseItem)&&
                                item.getBaseItem().getMaxInStack() != null &&
                                item.getItemsInStack() < item.getBaseItem().getMaxInStack())
                .toList();

        IngameItem oldIngameItem = ingameItems.stream().findFirst().orElse(null);

        boolean isEquipment = baseItem.getType().name().contains("EQUIPMENT");

        if (oldIngameItem == null) {
            IngameItem newItem = IngameItem.builder()
                    .baseItem(baseItem)
                    .user(user)
                    .itemsInStack(isEquipment ? null : 1L)
                    .sharpness(isEquipment ? 0L : null).build();
            ingameItemRepository.save(newItem);
        } else {
            oldIngameItem.setItemsInStack(oldIngameItem.getItemsInStack() + 1);
            ingameItemRepository.save(oldIngameItem);
        }

    }

    public static void userDied(User user) {
        user.setCurrentStamina(0L);
        userStaminaChanges(user, 0L);
        userRepository.save(user);
        var diedMessage = TelegramUtil.createMessageTemplate(user);
        diedMessage.setText("Вы умерли.");
        try {
            bot.execute(diedMessage);
        } catch (TelegramApiException e) {
            log.info(e.getMessage());
        }
    }

    public static long countExpToLevel(long level) {
        long base_exp = 20;
        double constant = 1.4;

        return (long) (base_exp * (Math.pow(level, constant)) - base_exp * level);

    }

    public static long countItemDamage(IngameItem ingameItem) {
        if (ingameItem.getBaseItem().getDamage() == null) {
            return 0;
        }
        float addDamage = ingameItem.getBaseItem().getDamage() > 5 ? (float) ingameItem.getBaseItem().getDamage() / 10 : (float) 0.5;

        return (long) (addDamage * ingameItem.getSharpness() + ingameItem.getBaseItem().getDamage());
    }

    public static long countDamage(User user) {
        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        var skills = appliedSkillRepository.findAllByUser(user);
        long sum = 0L;
        for (IngameItem item : items) {
            if (item.isEquipped()) {
                sum += countItemDamage(item);
            }
        }
        for (AppliedSkill skill : skills) {
            sum += (skill.getSkill().getDamageBonus() * skill.getSkillLevel());
        }
        return sum;
    }

    public static long countItemArmor(IngameItem ingameItem) {
        if (ingameItem.getBaseItem().getArmor() == null) {
            return 0;
        }
        float addArmor = ingameItem.getBaseItem().getArmor() > 5 ? (float) ingameItem.getBaseItem().getArmor() / 10 : (float) 0.5;

        return (long) (addArmor * ingameItem.getSharpness() + ingameItem.getBaseItem().getArmor());
    }

    public static long damageTakes(long armor, long damage) {
        float damageLeft = 1 - (float)((0.02 * armor) / (1 + 0.02 * armor));
        return (long) (damageLeft * damage);
    }

    public static long countArmor(User user) {
        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        var skills = appliedSkillRepository.findAllByUser(user);
        long sum = 0L;
        for (IngameItem item : items) {
            if (item.isEquipped()) {
                sum += countItemArmor(item);
            }
        }
        for (AppliedSkill skill : skills) {
            sum += (skill.getSkill().getArmorBonus() * skill.getSkillLevel());
        }
        return sum;
    }

    public static long countPrice(IngameItem item, long countItemsToSell) {
        return item.getBaseItem().getMaxInStack() == null ? item.getBaseItem().getBuyPrice() / 3 : item.getBaseItem().getBuyPrice() / 3 * countItemsToSell;
    }


    public static List<Class> getAvailableClasses(IngameItem item) {
        List<Class> result = new ArrayList<>();
        recGetAllAvailableClasses(item.getBaseItem().getClassRequired(), result);
        return result;
    }

    public static List<Class> getAvailableClasses(User user) {
        return classRepository.findAllByBaseClass(user.getUserClass()).stream().filter(w -> w.getRequiredLevel() <= user.getLevel()).toList();
    }

    public static List<Class> getAvailableClasses(Class baseClass) {
        List<Class> result = new ArrayList<>();
        recGetAllAvailableClasses(baseClass, result);
        return result;
    }

    private static void recGetAllAvailableClasses(Class baseClass, List<Class> result) {
        var classes = classRepository.findAllByBaseClass(baseClass);
        result.add(baseClass);
        for (Class item :
                classes) {
            recGetAllAvailableClasses(item, result);
        }
    }

    public static List<PartialBotApiMethod<? extends Serializable>> cancel(User user) {
        Fight fight = moveRepository.getMoveByUserId(user).orElseThrow().getFightId();
        User actor = fight.getUser1Id();
        User opponent = fight.getUser2Id();

        Move actorMove = moveRepository.getMoveByUserId(fight.getUser1Id()).orElse(null);
        Move opponentMove = moveRepository.getMoveByUserId(fight.getUser2Id()).orElse(null);

        actor.setUserState(BotState.NONE);
        opponent.setUserState(BotState.NONE);
        userRepository.save(actor);
        userRepository.save(opponent);

        moveRepository.delete(actorMove);
        moveRepository.delete(opponentMove);
        fightRepository.delete(fight);

        var messageToActor = createMessageTemplate(actor);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToActor.setReplyMarkup(createBaseReplyKeyboard());
        messageToOpponent.setReplyMarkup(createBaseReplyKeyboard());
        messageToActor.setText("Вызов отменен");
        messageToOpponent.setText("Вызов отменен");
        return List.of(messageToActor, messageToOpponent);
    }
}
