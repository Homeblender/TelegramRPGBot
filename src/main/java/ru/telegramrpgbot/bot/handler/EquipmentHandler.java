package ru.telegramrpgbot.bot.handler;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.utils.Pair;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.ItemType;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;

import java.io.Serializable;
import java.util.*;

import static ru.telegramrpgbot.bot.util.IngameUtil.*;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class EquipmentHandler implements Handler {

    private final IngameItemRepository ingameItemRepository;
    private final List<Pair<ItemType, String>> dictionary = new ArrayList<>();

    public EquipmentHandler(IngameItemRepository ingameItemRepository) {
        this.ingameItemRepository = ingameItemRepository;
        this.dictionary.add(new Pair<>(ItemType.EQUIPMENT_HELMET, String.format("%nШлем:%n")));
        this.dictionary.add(new Pair<>(ItemType.EQUIPMENT_BODY_ARMOR, String.format("%nНагрудный доспех:%n")));
        this.dictionary.add(new Pair<>(ItemType.EQUIPMENT_LEG_ARMOR, String.format("%nДоспех для ног:%n")));
        this.dictionary.add(new Pair<>(ItemType.EQUIPMENT_BOOTS, String.format("%nБотинки:%n")));

    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).split("_")[0].toUpperCase().equals(Command.EQUIP.name())) {
            return equipItem(user, message);
        } else if (message.substring(1).split("_")[0].toUpperCase().equals(Command.UNEQUIP.name())) {
            return unequipItem(user, message);
        }
        return showEquipment(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> unequipItem(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты.");
            return List.of(reply);
        }
        IngameItem item;
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id предмета.");
            return List.of(reply);
        }
        try {
            item = ingameItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        } catch (NumberFormatException exception) {
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }
        if (!item.isEquipped()) {
            reply.setText("Предмет не экипирован.");
            return List.of(reply);
        }
        item.setEquipped(false);
        ingameItemRepository.save(item);
        reply.setText(String.format("Вы сняли *%s*.", item.getBaseItem().getName()));
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> equipItem(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        IngameItem item;
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты.");
            return List.of(reply);
        }

        List<String> messageList = Arrays.stream(message.split("_")).toList();
        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id предмета.");
            return List.of(reply);
        }
        try {
            item = ingameItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        } catch (NumberFormatException exception) {
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }
        if (item.isEquipped()) {
            reply.setText("Предмет экипирован.");
            return List.of(reply);
        }
        if (!item.getBaseItem().getType().name().contains("EQUIPMENT")) {
            reply.setText("Нельзя экипировать предмет этого типа.");
            return List.of(reply);
        }
        if (!IngameUtil.getAllAvailableClasses(item).contains(user.getUserClass())) {
            reply.setText("Ваш класс не подходит для экипировки этого предмета.");
            return List.of(reply);
        }
        List<IngameItem> sameEquipmentItems = ingameItemRepository.findAllByUser(user).stream().filter(c -> c.getBaseItem().getType() == item.getBaseItem().getType() && c.isEquipped()).toList();
        List<IngameItem> equipped = ingameItemRepository.findAllByUser(user).stream().filter(IngameItem::isEquipped).toList();

        List<IngameItem> weapon_equipment = new ArrayList<>(equipped.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_ONE_HANDED_WEAPON).toList());
        List<IngameItem> shield_equipped = equipped.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_SHIELD).toList();

        List<IngameItem> two_handed_equipped = equipped.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_TWO_HANDED_WEAPON).toList();

        weapon_equipment.addAll(shield_equipped);

        if ((item.getBaseItem().getType() == ItemType.EQUIPMENT_ONE_HANDED_WEAPON
                || item.getBaseItem().getType() == ItemType.EQUIPMENT_SHIELD) && weapon_equipment.size() == 2) {
            reply.setText("Предмет этого типа уже экипирован.");
            return List.of(reply);
        }else if ((item.getBaseItem().getType() == ItemType.EQUIPMENT_ONE_HANDED_WEAPON
                ||item.getBaseItem().getType() == ItemType.EQUIPMENT_SHIELD) && !two_handed_equipped.isEmpty()) {
            reply.setText("Предмет этого типа уже экипирован.");
            return List.of(reply);
        }else if (item.getBaseItem().getType() == ItemType.EQUIPMENT_TWO_HANDED_WEAPON && !weapon_equipment.isEmpty()) {
            reply.setText("Предмет этого типа уже экипирован.");
            return List.of(reply);
        } else if (item.getBaseItem().getType() != ItemType.EQUIPMENT_ONE_HANDED_WEAPON && !sameEquipmentItems.isEmpty()) {
            reply.setText("Предмет этого типа уже экипирован.");
            return List.of(reply);
        } else if (!sameEquipmentItems.isEmpty()) {
            reply.setText("Предмет этого типа уже экипирован.");
            return List.of(reply);
        }
        item.setEquipped(true);
        ingameItemRepository.save(item);
        reply.setText(String.format("Вы экипировали *%s*.%n%n%s", item.getBaseItem().getName(),item.getBaseItem().getDescription()));
        return List.of(reply);
    }

    @SneakyThrows
    private List<PartialBotApiMethod<? extends Serializable>> showEquipment(User user) {
        SendMessage reply = createMessageTemplate(user);
        StringBuilder replyMessage = new StringBuilder(String.format("Ваша экипировка \uD83D\uDC5C:%n%nРуки:%n"));
        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        List<IngameItem> equipmentItems = items.stream().filter(c -> c.getBaseItem().getType().name().contains("EQUIPMENT") && c.isEquipped()).toList();
        List<IngameItem> sharpeningStones = ingameItemRepository.findAllByBaseItem_Type(ItemType.CONSUMABLE_SHARPENING_STONE);
        log.info(sharpeningStones.size()+"");
        List<IngameItem> weapon_equipment = new java.util.ArrayList<>(equipmentItems.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_ONE_HANDED_WEAPON).toList());
        List<IngameItem> shield_equipped = equipmentItems.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_SHIELD).toList();
        List<IngameItem> two_handed_equipped = equipmentItems.stream().filter(w -> w.getBaseItem().getType() == ItemType.EQUIPMENT_TWO_HANDED_WEAPON).toList();

        weapon_equipment.addAll(shield_equipped);
        weapon_equipment.addAll(two_handed_equipped);

        for (IngameItem item : weapon_equipment) {
            replyMessage.append(createItemStats(item));
            if (!sharpeningStones.isEmpty())
                replyMessage.append(sharpeningTemplate(item));
        }
        for (Pair item :
                dictionary) {
            replyMessage.append(item.getSecond());
            try {
                replyMessage.append(createItemStats(equipmentItems.stream().filter(c -> c.getBaseItem().getType() == item.getFirst()).findFirst().orElseThrow()));
                if (!sharpeningStones.isEmpty())
                    replyMessage.append(sharpeningTemplate(equipmentItems.stream().filter(c -> c.getBaseItem().getType() == item.getFirst()).findFirst().orElseThrow()));
            } catch (Exception ignored) {
            }
        }


        reply.setText(replyMessage.toString());
        return List.of(reply);
    }
    private String sharpeningTemplate(IngameItem ingameItem) {
        return String.format("Заточить \uD83D\uDD28 - /sharp\\_%d%n", ingameItem.getId());
    }
    private String createItemStats(IngameItem ingameItem) {
        double damage = countItemDamage(ingameItem);
        double armor = countItemArmor(ingameItem);

        return (String.format(
                "+%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s* (+%d)%nСнять - /unequip\\_%d%n",
                Math.round(damage),
                Math.round(armor),
                ingameItem.getBaseItem().getName(),
                ingameItem.getSharpness(),
                ingameItem.getId()
        ));
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.EQUIP, Command.EQUIPMENT, Command.UNEQUIP);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
