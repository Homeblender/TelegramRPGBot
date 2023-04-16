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
public class IngameUtil {
    //TODO узнать как лучше сделать
    private static UserRepository userRepository;
    private static IngameItemRepository ingameItemRepository;
    private static InventoryCellRepository inventoryCellRepository;

    public IngameUtil(UserRepository userRepository, IngameItemRepository ingameItemRepository, InventoryCellRepository inventoryCellRepository) {
        IngameUtil.userRepository = userRepository;
        IngameUtil.ingameItemRepository = ingameItemRepository;
        IngameUtil.inventoryCellRepository = inventoryCellRepository;
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
    }

    public static void userGetItem(User user, BaseItem baseItem) {
        var inventoryCells = inventoryCellRepository.findAllByUser(user);
        inventoryCells = inventoryCells.stream()
                .filter(cell ->
                        cell.getItem().getBaseItem().getMaxInStack() != null &&
                        cell.getItem().getBaseItem() == baseItem &&
                        cell.getItem().getItemsInStack() < cell.getItem().getBaseItem().getMaxInStack())
                .toList();

        InventoryCell inventoryCell = inventoryCells.stream().findFirst().orElse(null);
        if (inventoryCell == null) {
            IngameItem newItem = IngameItem.builder().baseItem(baseItem).build();
            ingameItemRepository.save(newItem);
            InventoryCell newInventoryCell = InventoryCell.builder().item(newItem).user(user).build();
            inventoryCellRepository.save(newInventoryCell);
        } else {
            inventoryCell.getItem().setItemsInStack(inventoryCell.getItem().getItemsInStack() + 1);
            ingameItemRepository.save(inventoryCell.getItem());
            inventoryCellRepository.save(inventoryCell);
        }

    }

    public static void userDied(User user) {
        user.setCurrentStamina(0L);
        userRepository.save(user);
    }

    public static long countPrice(IngameItem ingameItem){
        return ingameItem.getBaseItem().getBuyPrice()/3;
    }
}
