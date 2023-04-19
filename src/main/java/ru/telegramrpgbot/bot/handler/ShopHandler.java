package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.ItemType;
import ru.telegramrpgbot.model.BaseItem;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.BaseItemRepository;

import java.io.Serializable;
import java.util.*;

import static ru.telegramrpgbot.bot.util.IngameUtil.userGetItem;
import static ru.telegramrpgbot.bot.util.IngameUtil.userGoldChanges;
import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class ShopHandler implements Handler {

    private final BaseItemRepository baseItemRepository;
    private final Map<ItemType, String> mapNames = new HashMap<>();
    private final List<String> WEAPON_LIST = new ArrayList<>();
    private final List<String> ARMOR_LIST = new ArrayList<>();
    private final List<String> CALLBACK_ITEMS = new ArrayList<>();

    public ShopHandler(BaseItemRepository baseItemRepository) {
        this.baseItemRepository = baseItemRepository;
        mapNames.put(ItemType.EQUIPMENT_HELMET, "шлемы");
        mapNames.put(ItemType.EQUIPMENT_BODY_ARMOR, "нагрудные достпехи");
        mapNames.put(ItemType.EQUIPMENT_LEG_ARMOR, "ножные доспехи");
        mapNames.put(ItemType.EQUIPMENT_BOOTS, "ботинки");
        mapNames.put(ItemType.EQUIPMENT_ONE_HANDED_WEAPON, "одноручные оружия");
        mapNames.put(ItemType.EQUIPMENT_TWO_HANDED_WEAPON, "двуручные оружия");
        mapNames.put(ItemType.EQUIPMENT_SHIELD, "щиты");

        WEAPON_LIST.add(ItemType.EQUIPMENT_ONE_HANDED_WEAPON.name());
        WEAPON_LIST.add(ItemType.EQUIPMENT_TWO_HANDED_WEAPON.name());
        WEAPON_LIST.add(ItemType.EQUIPMENT_SHIELD.name());

        ARMOR_LIST.add(ItemType.EQUIPMENT_HELMET.name());
        ARMOR_LIST.add(ItemType.EQUIPMENT_BODY_ARMOR.name());
        ARMOR_LIST.add(ItemType.EQUIPMENT_LEG_ARMOR.name());
        ARMOR_LIST.add(ItemType.EQUIPMENT_BOOTS.name());
        ARMOR_LIST.add(ItemType.EQUIPMENT_BOOTS.name());

        CALLBACK_ITEMS.addAll(ARMOR_LIST);
        CALLBACK_ITEMS.addAll(WEAPON_LIST);
    }
    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        Command messageCommand = null;
        try {
            messageCommand = Arrays.stream(Command.values()).filter(command -> command.getRussian().toUpperCase().equals(message.toUpperCase().split(" ")[0])).findFirst().orElseThrow();
        }catch (Exception ignored){}
        if (user.getUserState() != BotState.NONE) {
            return busyReply(user);
        }else if (WEAPON_LIST.contains(message.toUpperCase())) {
            return shopSeeWeapon(user, message);
        }else if (ARMOR_LIST.contains(message.toUpperCase())) {
            return shopSeeArmor(user, message);
        }else if (message.toUpperCase().substring(1).split("_")[0].equals(Command.BUY.name())) {
            return buyItem(user, message);
        }else {
            assert messageCommand != null;
            if (messageCommand.equals(Command.SHOP)) {
                return shopReply(user);
            } else if (messageCommand.equals(Command.WEAPON_AND_SHIELDS)) {
                return shopSeeWeapon(user, null);
            }  else if (messageCommand.equals(Command.ARMOR)) {
                return shopSeeArmor(user,null);
            }
        }return shopReply(user);
    }
    private List<PartialBotApiMethod<? extends Serializable>> buyItem(User user, String message) {
        var reply = createMessageTemplate(user);

        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        List<String> messageList = Arrays.stream(message.split("_")).toList();

        reply.setReplyMarkup(createBaseReplyKeyboard());

        BaseItem item;

        if (messageList.size() != 2 || messageList.get(1).length() < 1) {
            reply.setText("Не указан id предмета");
            return List.of();
        }

        try {
            item = baseItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        } catch (NumberFormatException exception) {
            reply.setText("Нет такого предмета.");
            return List.of(reply);
        }
        if (user.getGold()<item.getBuyPrice()){
            reply.setText("Недостаточно денег.");
            return List.of(reply);
        }
        reply.setText(String.format("Вы успешно купили *%s*.",item.getName()));

        userGetItem(user,item);
        userGoldChanges(user,-item.getBuyPrice());
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> shopSeeWeapon(User user, String string) {
        if (WEAPON_LIST.contains(string)){
            return List.of(getWeaponMessage(user,ItemType.valueOf(string)));
        }
        return List.of(getWeaponMessage(user,ItemType.EQUIPMENT_ONE_HANDED_WEAPON));
    }
    private List<PartialBotApiMethod<? extends Serializable>> shopSeeArmor(User user, String string) {
        if (ARMOR_LIST.contains(string)){
            return List.of(getArmorMessage(user,ItemType.valueOf(string)));
        }
        return List.of(getArmorMessage(user,ItemType.EQUIPMENT_HELMET));
    }
    private String getMessage(User user, ItemType itemType){
        List<Class> availableClasses = new ArrayList<>();
        List<BaseItem> baseItemList = new ArrayList<>();

        for (Class userClass = user.getUserClass(); userClass != null; userClass = userClass.getBaseClass()) {
            availableClasses.add(userClass);
        }
        for (Class baseClass :
                availableClasses) {
            baseItemList.addAll(baseItemRepository.findAllByClassRequiredAndIsForSaleAndType(baseClass, true, itemType));
        }
        StringBuilder replyMessage = new StringBuilder();

        replyMessage.append(!baseItemList.isEmpty()?String.format("Доступные вам *%s*:%n", mapNames.get(itemType)):"Нет доступных предметов.");

        for (BaseItem base :
                baseItemList) {
            replyMessage.append(String.format(
                    "+%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s*%n",
                    base.getDamage() == null ? 0 : base.getDamage(),
                    base.getArmor() == null ? 0 : base.getArmor(),
                    base.getName()));
            replyMessage.append(String.format("Купить за \uD83D\uDCB0 %d - /buy\\_%d%n%n",base.getBuyPrice(),base.getId()));
        }
        return replyMessage.toString();
    }

    private SendMessage getArmorMessage(User user, ItemType itemType) {
        var reply = createMessageTemplate(user);
        reply.setText(getMessage(user,itemType));


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();

        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Шлемы", ARMOR_LIST.get(0)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Нагрудники", ARMOR_LIST.get(1)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Поножи", ARMOR_LIST.get(2)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Ботинки", ARMOR_LIST.get(3)));

        if (itemType.equals(ItemType.EQUIPMENT_HELMET)){
            inlineKeyboardButtonsRowOne.remove(0);
        } else if (itemType.equals(ItemType.EQUIPMENT_BODY_ARMOR)) {
            inlineKeyboardButtonsRowOne.remove(1);
        } else if (itemType.equals(ItemType.EQUIPMENT_LEG_ARMOR)) {
            inlineKeyboardButtonsRowOne.remove(2);
        } else if (itemType.equals(ItemType.EQUIPMENT_BOOTS)) {
            inlineKeyboardButtonsRowOne.remove(3);
        }
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return reply;
    }

    private SendMessage getWeaponMessage(User user, ItemType itemType) {
        var reply = createMessageTemplate(user);
        reply.setText(getMessage(user,itemType));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();

        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Одноручное Оружие", WEAPON_LIST.get(0)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Двуручное Оружие", WEAPON_LIST.get(1)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Щиты", WEAPON_LIST.get(2)));
        if (itemType.equals(ItemType.EQUIPMENT_ONE_HANDED_WEAPON)){
            inlineKeyboardButtonsRowOne.remove(0);

        } else if (itemType.equals(ItemType.EQUIPMENT_TWO_HANDED_WEAPON)) {
            inlineKeyboardButtonsRowOne.remove(1);
        } else if (itemType.equals(ItemType.EQUIPMENT_SHIELD)) {
            inlineKeyboardButtonsRowOne.remove(2);
        }
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return reply;
    }


    private List<PartialBotApiMethod<? extends Serializable>> shopReply(User user) {
        var buttons = new KeyboardButton[]{
                new KeyboardButton("\uD83D\uDDE1 Оружие и щиты"),
                new KeyboardButton("\uD83C\uDFBD Броня"),
                new KeyboardButton("⬅️ Город")};

        var reply = createMessageTemplate(user);
        reply.setText("Выбери нужную тебе категорию и сможешь купить предметы, подходящие тебе по *классу*.");
        reply.setReplyMarkup(createKeyboard(buttons));

        return List.of(reply);
    }



    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.SHOP, Command.WEAPON_AND_SHIELDS, Command.ARMOR, Command.BUY);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return CALLBACK_ITEMS;
    }
}
