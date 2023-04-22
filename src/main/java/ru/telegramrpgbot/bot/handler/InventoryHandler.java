package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.ConsumableItemEffect;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.ConsumableItemEffectRepository;
import ru.telegramrpgbot.repository.IngameItemRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.IngameUtil.*;
import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class InventoryHandler implements Handler {

    private final IngameItemRepository ingameItemRepository;
    private final UserRepository userRepository;
    private final ConsumableItemEffectRepository consumableItemEffectRepository;
    private final List<String> CALLBACK_LIST = new ArrayList<>();


    public InventoryHandler(IngameItemRepository ingameItemRepository, UserRepository userRepository, ConsumableItemEffectRepository consumableItemEffectRepository) {
        this.ingameItemRepository = ingameItemRepository;
        this.userRepository = userRepository;
        this.consumableItemEffectRepository = consumableItemEffectRepository;
        CALLBACK_LIST.add("EQUIPMENT");
        CALLBACK_LIST.add("CONSUMABLE");
        CALLBACK_LIST.add("MATERIAL");
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.equals("EQUIPMENT")) {
            return showEquipmentInventory(user);
        } else if (message.equals("CONSUMABLE")) {
            return showConsumableInventory(user);
        } else if (message.equals("MATERIAL")) {
            return showMaterialInventory(user);
        } else if (message.toUpperCase().contains(Command.SELL.name())) {
            return sellItem(user, message);
        }else if (message.toUpperCase().contains(Command.USE.name())) {
            return useItem(user, message);
        }
        return showEquipmentInventory(user);

    }

    private List<PartialBotApiMethod<? extends Serializable>> useItem(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        IngameItem item;
        ConsumableItemEffect itemEffect;
        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id предмета");
            return List.of();
        }
        try {
            item = ingameItemRepository.findAllByIdAndUser(Long.parseLong(messageList.get(1)),user);
        } catch (Exception exception) {
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }
        try {
            itemEffect = consumableItemEffectRepository.findByBaseItem(item.getBaseItem()).orElseThrow();
        } catch (Exception exception) {
            reply.setText("Нельзя применить этот предмет.");
            return List.of(reply);
        }

        userHealthChanges(user,itemEffect.getAddLife());
        userManaChanges(user,itemEffect.getAddMana());
        userStaminaChanges(user,itemEffect.getAddStamina());
        userRepository.save(user);

        reply.setText(String.format("Вы успешно применили *%s*.",item.getBaseItem().getName()));

        if (item.getItemsInStack() == 1) ingameItemRepository.delete(item);
        else{
            item.setItemsInStack(item.getItemsInStack()-1);
            ingameItemRepository.save(item);
        }
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> sellItem(User user, String message) {
        var reply = createMessageTemplate(user);

        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        List<String> messageList = Arrays.stream(message.split("_")).toList();

        reply.setReplyMarkup(createBaseReplyKeyboard());
        long countItemsToSell;

        IngameItem item;

        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id предмета");
            return List.of();
        }
        try {
            countItemsToSell = messageList.size() == 3 ? Long.parseLong(messageList.get(2)) : 1;
        } catch (NumberFormatException exception) {
            reply.setText("Колличество предметов должно быть числом.");
            return List.of(reply);
        }

        try {
            item = ingameItemRepository.findAllByIdAndUser(Long.parseLong(messageList.get(1)),user);
        } catch (Exception exception) {
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }
        if (item.isEquipped()) {
            reply.setText("Нельзя продать экипированный предмет!");
            return List.of(reply);
        }
        long cost = IngameUtil.countPrice(item, countItemsToSell);

        if (item.getItemsInStack() != null && item.getItemsInStack() > countItemsToSell) {
            item.setItemsInStack(item.getItemsInStack() - countItemsToSell);
            ingameItemRepository.save(item);
        } else if (item.getItemsInStack() != null && item.getItemsInStack() < countItemsToSell) {
            reply.setText("У вас не достаточно таких предметов в одной ячейке.");
            return List.of(reply);
        } else if (item.getItemsInStack() != null && item.getItemsInStack() == countItemsToSell) {
            ingameItemRepository.delete(item);
        } else if (item.getItemsInStack() == null) {
            ingameItemRepository.delete(item);
        }

        reply.setText(String.format("Вы успешно продали x%d *%s* за %d\uD83D\uDCB0.", countItemsToSell, item.getBaseItem().getName(), cost));
        userGoldChanges(user, cost);

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showEquipmentInventory(User user) {
        var items = ingameItemRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);

        var equipmentItems = items.stream().filter(c -> c.getBaseItem().getType().name().contains("EQUIPMENT") && !c.isEquipped()).toList();
        if (equipmentItems.size() > 0) {
            replyMessage.append("Экипировка ⚔️:\n\n");
            for (IngameItem ingameItem : equipmentItems) {
                double damage = countItemDamage(ingameItem);
                double armor = countItemArmor(ingameItem);

                replyMessage.append(String.format(
                        "(id: %s)+%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s* (+%d)%n",
                        ingameItem.getId(),
                        Math.round(damage),
                        Math.round(armor),
                        ingameItem.getBaseItem().getName(),
                        ingameItem.getSharpness()
                ));
                if (IngameUtil.getAvailableClasses(ingameItem).contains(user.getUserClass())) {
                    replyMessage.append(equipTemplate(ingameItem));
                }else replyMessage.append(String.format("Минимальный требуемый класс - _%s_%n",ingameItem.getBaseItem().getClassRequired().getName()));
                if (ingameItem.getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(sellTemplate(ingameItem));
                }
            }
        } else replyMessage.append("У тебя нет предметов которые ты можешь экипировать.");


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Используемое", CALLBACK_LIST.get(1)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Материалы", CALLBACK_LIST.get(2)));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));

        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText(replyMessage.toString());
        return List.of(reply);


    }


    private List<PartialBotApiMethod<? extends Serializable>> showConsumableInventory(User user) {
        var ingameItems = ingameItemRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);


        var consumableItems = ingameItems.stream().filter(c -> c.getBaseItem().getType().name().contains("CONSUMABLE")).toList();
        if (consumableItems.size() > 0) {
            replyMessage.append(String.format("%nИспользуемые предметы \uD83C\uDF77:%n%n"));

            for (IngameItem ingameItem : consumableItems) {
                var consumableItemEffect = consumableItemEffectRepository.findByBaseItem(ingameItem.getBaseItem()).orElse(null);
                replyMessage.append(String.format(
                        "(id: %s) *%s* x%d%n%s%n%s",
                        ingameItem.getId(),
                        ingameItem.getBaseItem().getName(),
                        ingameItem.getItemsInStack(),
                        ingameItem.getBaseItem().getDescription(),
                        consumableItemEffect!=null?String.format("Использовать предмет - /use\\_%d%n",ingameItem.getId()) : ""
                ));
                if (ingameItem.getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(sellTemplate(ingameItem));
                }
            }
        } else replyMessage.append("У вас нет используемых предметов.");


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Экпипировка", CALLBACK_LIST.get(0)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Материалы", CALLBACK_LIST.get(2)));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));

        reply.setReplyMarkup(inlineKeyboardMarkup);

        reply.setText(replyMessage.toString());
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showMaterialInventory(User user) {


        var ingameItems = ingameItemRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);


        var materialItems = ingameItems.stream().filter(c -> c.getBaseItem().getType().name().contains("MATERIAL")).toList();
        if (materialItems.size() > 0) {
            replyMessage.append(String.format("%nМатериалы \uD83E\uDEA8:%n%n"));

            for (IngameItem ingameItem : materialItems) {
                replyMessage.append(String.format(
                        "(id: %s) *%s* x%d%n",
                        ingameItem.getId(),
                        ingameItem.getBaseItem().getName(),
                        ingameItem.getItemsInStack()
                ));
                if (ingameItem.getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(sellTemplate(ingameItem));
                }
            }
        } else replyMessage.append("У вас нет материалов.");


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Экпипировка", CALLBACK_LIST.get(0)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Используемое", CALLBACK_LIST.get(1)));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));

        reply.setReplyMarkup(inlineKeyboardMarkup);

        reply.setText(replyMessage.toString());
        return List.of(reply);


    }

    private String sellTemplate(IngameItem ingameItem) {
        return String.format("Продать x1 за %d\uD83D\uDCB0 - /sell\\_%d%n%n", countPrice(ingameItem, 1), ingameItem.getId());
    }

    private String equipTemplate(IngameItem ingameItem) {
        return String.format("Экипировать - /equip\\_%d%n", ingameItem.getId());
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.INVENTORY, Command.SELL, Command.USE);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return CALLBACK_LIST;
    }
}
