package ru.telegramrpgbot.bot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegramrpgbot.bot.Bot;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.ClassRepository;
import ru.telegramrpgbot.repository.IngameItemRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class IngameUtil {
    private static UserRepository userRepository;
    private static IngameItemRepository ingameItemRepository;
    private static ClassRepository classRepository;
    private static Bot bot;

    public IngameUtil(UserRepository userRepository, IngameItemRepository ingameItemRepository, Bot bot, ClassRepository classRepository) {
        IngameUtil.userRepository = userRepository;
        IngameUtil.ingameItemRepository = ingameItemRepository;
        IngameUtil.classRepository = classRepository;
        IngameUtil.bot = bot;
    }

    public static Long userHealthChanges(User user, Long healthChanged) {
        user.setCurrentHealth(user.getCurrentHealth() + healthChanged);
        if (user.getCurrentHealth() > user.getMaxHealth()) user.setMaxHealth(user.getCurrentHealth());
        else if (user.getCurrentHealth() < user.getMaxHealth()) {
            user.setMaxHealth(0L);
            userDied(user);
        }
        userRepository.save(user);
        return user.getCurrentHealth();
    }

    public static void userManaChanges(User user, Long manaChanged) {
        user.setCurrentMana(user.getCurrentMana() + manaChanged);
        if (user.getCurrentMana() > user.getMaxMana()) user.setMaxMana(user.getCurrentMana());
        else if (user.getCurrentMana() < user.getMaxMana()) {
            user.setMaxMana(0L);
            userDied(user);
        }
        userRepository.save(user);
    }

    public static void userStaminaChanges(User user, Long staminaChanged) {
        user.setCurrentStamina(user.getCurrentStamina() + staminaChanged);
        if (user.getCurrentStamina() > user.getMaxStamina()) user.setCurrentStamina(user.getCurrentStamina());
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
        if (user.getExp() > countExpToLevel(user.getLevel()+1)){
            user.setExp(user.getExp()- countExpToLevel(user.getLevel()+1));

            var levelUpMessage = TelegramUtil.createMessageTemplate(user);
            levelUpMessage.setText("Ты достиг нового уровня и получил одно очко пассивных умений /passives");
            try {
                bot.execute(levelUpMessage);
            } catch (TelegramApiException e) {
                log.info(e.getMessage());
            }
            user.setLevel(user.getLevel()+1);
            user.setPassivePoints(user.getPassivePoints()+1);
            userRepository.save(user);
            levelUp(user);
        }

    }

    public static void userGetItem(User user, BaseItem baseItem) {
        var ingameItems = ingameItemRepository.findAllByUser(user);
        ingameItems = ingameItems.stream()
                .filter(item ->
                        item.getBaseItem() == baseItem &&
                        item.getBaseItem().getMaxInStack() != null &&
                        item.getItemsInStack() < item.getBaseItem().getMaxInStack())
                .toList();

        IngameItem oldIngameItem = ingameItems.stream().findFirst().orElse(null);

        boolean isEquipment = baseItem.getType().name().contains("EQUIPMENT");

        if (oldIngameItem == null) {
            IngameItem newItem = IngameItem.builder()
                    .baseItem(baseItem)
                    .user(user)
                    .itemsInStack(isEquipment?null:1L)
                    .sharpness(isEquipment? 0L: null).build();
            ingameItemRepository.save(newItem);
        } else {
            oldIngameItem.setItemsInStack(oldIngameItem.getItemsInStack() + 1);
            ingameItemRepository.save(oldIngameItem);
        }

    }

    public static void userDied(User user) {
        user.setCurrentStamina(0L);
        userRepository.save(user);
    }

    public static long countExpToLevel(long level) {
        long base_exp = 20;
        double constant = 1.4;

        return (long) (base_exp*(Math.pow(level,constant)) - base_exp*level);

    }
    public static long countItemDamage(IngameItem ingameItem) {
        if(ingameItem.getBaseItem().getDamage() == null){
            return 0;
        }
        float addDamage = ingameItem.getBaseItem().getDamage() > 5? (float)ingameItem.getBaseItem().getDamage() /10: (float) 0.5;

        return (long) (addDamage * ingameItem.getSharpness() + ingameItem.getBaseItem().getDamage());
    }
    public  static long countDamage(User user) {
        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        long sum = 0L;
        for (IngameItem item : items) {
            if (item.isEquipped()) {
                sum+=countItemDamage(item);
            }
        }
        return sum;
    }
    public static long countItemArmor(IngameItem ingameItem) {
        if(ingameItem.getBaseItem().getArmor() == null){
            return 0;
        }
        float addArmor = ingameItem.getBaseItem().getArmor() > 5? (float)ingameItem.getBaseItem().getArmor() /10: (float) 0.5;

        return (long) (addArmor * ingameItem.getSharpness() + ingameItem.getBaseItem().getArmor());
    }

    public static long countArmor(User user) {
        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        long sum = 0L;
        for (IngameItem item : items) {
            if (item.isEquipped()) {
                sum+=countItemArmor(item);
            }
        }
        return sum;
    }

    public static long countPrice(IngameItem item, long countItemsToSell){
        return item.getBaseItem().getMaxInStack() == null ? item.getBaseItem().getBuyPrice() / 3 : item.getBaseItem().getBuyPrice() / 3 * countItemsToSell;
    }


    public static List<Class> getAllAvailableClasses(IngameItem item){
        List<Class> result = new ArrayList<>();
        recGetAllAvailableClasses(item.getBaseItem().getClassRequired(), result);
        return result;
    }

    public static List<Class> getAllAvailableClasses(User user){
        return classRepository.findAllByBaseClass(user.getUserClass()).stream().filter(w-> w.getRequiredLevel()<=user.getLevel()).toList();
    }


    private static void recGetAllAvailableClasses(Class baseClass, List<Class> result){
        var classes = classRepository.findAllByBaseClass(baseClass);
        result.add(baseClass);
        for (Class item :
                classes) {
            recGetAllAvailableClasses(item, result);
        }
    }
}
