package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.InventoryCell;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;
import ru.telegramrpgbot.repository.InventoryCellRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;
import static ru.telegramrpgbot.bot.util.UserChangesUtil.userGoldChanges;

@Component
@Slf4j
public class InventoryHandler implements Handler {

    private final InventoryCellRepository inventoryCellRepository;
    private final IngameItemRepository ingameItemRepository;
    private final List<String> CALLBACK_LIST = new ArrayList<>();


    public InventoryHandler(InventoryCellRepository inventoryCellRepository, IngameItemRepository ingameItemRepository) {
        this.inventoryCellRepository = inventoryCellRepository;
        this.ingameItemRepository = ingameItemRepository;
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
            log.info(message);
            return sellItem(user, message);
        }
        return showEquipmentInventory(user);

    }

    private List<PartialBotApiMethod<? extends Serializable>> sellItem(User user, String message) {
        List<String> messageList = Arrays.stream(message.split(" ")).toList();

        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(createBaseReplyKeyboard());
        long countItemsToSell;
        InventoryCell cell;
        IngameItem item;

        if(messageList.size()<2){
            reply.setText(sellTemplate());
            return List.of();
        }
        try {
            countItemsToSell = messageList.size() == 3 ? Long.parseLong(messageList.get(2)) : 1;
            log.info(countItemsToSell +"");
        } catch (NumberFormatException exception) {
            reply.setText("Колличество предметов должно быть числом.");
            return List.of(reply);
        }

        try {
            cell = inventoryCellRepository.findAllByInventoryCellId(Long.parseLong(messageList.get(1)));
        } catch (NumberFormatException exception) {
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }
        try {
            item = cell.getItem();
        }catch (NullPointerException exception){
            reply.setText("У вас нет таких предметов.");
            return List.of(reply);
        }


        long cost = item.getBaseItem().getMaxInStack() == null ? item.getBaseItem().getBuyPrice() / 3 : item.getBaseItem().getBuyPrice() / 3 * countItemsToSell;

        if (cell.getItem().getItemsInStack() != null && cell.getItem().getItemsInStack() < countItemsToSell) {
            item.setItemsInStack(item.getItemsInStack() - countItemsToSell);
            inventoryCellRepository.save(cell);
            ingameItemRepository.save(item);
        } else if (cell.getItem().getItemsInStack() != null && cell.getItem().getItemsInStack() == countItemsToSell) {
            inventoryCellRepository.delete(cell);
            ingameItemRepository.delete(item);
        } else if (cell.getItem().getItemsInStack() == null) {
            inventoryCellRepository.delete(cell);
            ingameItemRepository.delete(item);
        }

        reply.setText(String.format("Вы успешно продали x%d *%s* за %d\uD83D\uDCB0.", countItemsToSell, item.getBaseItem().getName(), cost));
        userGoldChanges(user, cost);

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showEquipmentInventory(User user) {
        var inventoryCells = inventoryCellRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);

        var equipmentItems = inventoryCells.stream().filter(c -> c.getItem().getBaseItem().getType().name().contains("EQUIPMENT")).toList();
        if (equipmentItems.size() > 0) {
            replyMessage.append("Предметы которые ты можешь экипировать ⚔️:\n\n");
            for (InventoryCell inventoryCell : equipmentItems) {
                double damage = inventoryCell.getItem().getBaseItem().getDamage() == null ?
                        0 :
                        ((double) inventoryCell.getItem().getSharpness() / 10 + 1) * inventoryCell.getItem().getBaseItem().getDamage();

                double armor = inventoryCell.getItem().getBaseItem().getArmor() == null ?
                        0 :
                        ((double) inventoryCell.getItem().getSharpness() / 10 + 1) * inventoryCell.getItem().getBaseItem().getArmor();

                replyMessage.append(String.format(
                        "+%d\uD83D\uDDE1 +%d\uD83C\uDFBD *%s* (+%d)%n",
                        Math.round(damage),
                        Math.round(armor),
                        inventoryCell.getItem().getBaseItem().getName(),
                        inventoryCell.getItem().getSharpness()
                ));
                if (inventoryCell.getItem().getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(String.format("Цена при продаже - %d\uD83D\uDCB0 (id = %d)%n", inventoryCell.getItem().getBaseItem().getBuyPrice() / 3, inventoryCell.getInventoryCellId()));
                }

            }
            replyMessage.append(sellTemplate());
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
        var inventoryCells = inventoryCellRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);


        var consumableItems = inventoryCells.stream().filter(c -> c.getItem().getBaseItem().getType().name().contains("CONSUMABLE")).toList();
        if (consumableItems.size() > 0) {
            replyMessage.append(String.format("%nИспользуемые предметы \uD83C\uDF77:%n%n"));

            for (InventoryCell inventoryCell : consumableItems) {
                replyMessage.append(String.format(
                        "*%s* x%d%n",
                        inventoryCell.getItem().getBaseItem().getName(),
                        inventoryCell.getItem().getItemsInStack()
                ));
                if (inventoryCell.getItem().getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(String.format("Цена при продаже - %d\uD83D\uDCB0 (id = %d)%n", inventoryCell.getItem().getBaseItem().getBuyPrice() / 3, inventoryCell.getInventoryCellId()));
                }
            }
            replyMessage.append(sellTemplate());
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


        var inventoryCells = inventoryCellRepository.findAllByUser(user);
        StringBuilder replyMessage = new StringBuilder();
        var reply = createMessageTemplate(user);


        var materialItems = inventoryCells.stream().filter(c -> c.getItem().getBaseItem().getType().name().contains("MATERIAL")).toList();
        if (materialItems.size() > 0) {
            replyMessage.append(String.format("%nМатериалы \uD83E\uDEA8:%n%n"));

            for (InventoryCell inventoryCell : materialItems) {
                replyMessage.append(String.format(
                        "*%s* x%d%n",
                        inventoryCell.getItem().getBaseItem().getName(),
                        inventoryCell.getItem().getItemsInStack()
                ));
                if (inventoryCell.getItem().getBaseItem().getBuyPrice() != null) {
                    replyMessage.append(String.format("Цена при продаже - %d\uD83D\uDCB0 (id = %d)%n", inventoryCell.getItem().getBaseItem().getBuyPrice() / 3, inventoryCell.getInventoryCellId()));
                }
            }
            replyMessage.append(sellTemplate());
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

    private String sellTemplate() {
        return String.format("%n/sell {id} {колличество} - продать предмет ");
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.INVENTORY, Command.SELL);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return CALLBACK_LIST;
    }
}
