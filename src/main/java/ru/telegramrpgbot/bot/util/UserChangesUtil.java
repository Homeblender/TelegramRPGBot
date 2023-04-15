package ru.telegramrpgbot.bot.util;

import org.springframework.stereotype.Component;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.InventoryCell;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;
import ru.telegramrpgbot.repository.InventoryCellRepository;
import ru.telegramrpgbot.repository.UserRepository;

@Component
public class UserChangesUtil {
    //TODO спросить у ярика как лучше сделать
    private static UserRepository userRepository;
    private static IngameItemRepository ingameItemRepository;
    private static InventoryCellRepository inventoryCellRepository;
    public UserChangesUtil(UserRepository userRepository, IngameItemRepository ingameItemRepository, InventoryCellRepository inventoryCellRepository){
        UserChangesUtil.userRepository = userRepository;
        UserChangesUtil.ingameItemRepository = ingameItemRepository;
        UserChangesUtil.inventoryCellRepository = inventoryCellRepository;
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
    public static Long userManaChanges(User user, Long manaChanged) {
        user.setCurrentMana(user.getCurrentMana() + manaChanged);
        if (user.getCurrentMana() > user.getMaxMana()) user.setMaxMana(user.getCurrentMana());
        else if (user.getCurrentMana() < user.getMaxMana()) {
            user.setMaxMana(0L);
            userDied(user);
        }
        userRepository.save(user);
        return user.getCurrentMana();
    }
    public static Long userStaminaChanges(User user, Long staminaChanged) {
        user.setCurrentStamina(user.getCurrentStamina() + staminaChanged);
        if (user.getCurrentStamina() > user.getMaxStamina()) user.setCurrentStamina(user.getCurrentStamina());
        userRepository.save(user);
        return user.getCurrentStamina();
    }
    public static Long userGoldChanges(User user, Long goldChanged) {
        user.setGold(user.getGold() + goldChanged);
        userRepository.save(user);
        return user.getGold();
    }
    public static Long userExpChanged(User user, Long expChanged) {
        user.setExp(user.getExp() + expChanged);
        userRepository.save(user);
        return user.getGold();
    }
    public static void userGetItem(User user, BaseItem baseItem) {
        IngameItem newItem = IngameItem.builder().itemId(baseItem).build();
        ingameItemRepository.save(newItem);
        InventoryCell inventoryCell = InventoryCell.builder().itemId(newItem).userId(user).build();
        inventoryCellRepository.save(inventoryCell);
    }
    public static void userDied(User user){
        user.setCurrentStamina(0L);
        userRepository.save(user);
    }
}
