package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.model.Class;
import ru.telegramrpgbot.model.*;
import ru.telegramrpgbot.repository.BaseItemCraftRepository;
import ru.telegramrpgbot.repository.BaseItemRepository;
import ru.telegramrpgbot.repository.ClassRepository;
import ru.telegramrpgbot.repository.IngameItemRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.telegramrpgbot.bot.util.IngameUtil.userGetItem;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class ForgeHandler implements Handler {
    private final BaseItemRepository baseItemRepository;
    private final List<String> CLASS_LIST = new ArrayList<>();
    private final BaseItemCraftRepository baseItemCraftRepository;
    private final IngameItemRepository ingameItemRepository;
    private final ClassRepository classRepository;

    public ForgeHandler(BaseItemRepository baseItemRepository, BaseItemCraftRepository baseItemCraftRepository, IngameItemRepository ingameItemRepository, ClassRepository classRepository) {
        this.baseItemRepository = baseItemRepository;
        this.baseItemCraftRepository = baseItemCraftRepository;
        this.ingameItemRepository = ingameItemRepository;
        this.classRepository = classRepository;
        CLASS_LIST.add("⚔ Воин");
        CLASS_LIST.add("🏹 Рейнджер");
        CLASS_LIST.add("💫 Маг");
    }


    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).toUpperCase().split("_")[0].equals(Command.CRAFT.name())){
            return craftItem(user,message);

        }else if (CLASS_LIST.contains(message)){
            return showBaseForge(user, message);
        }
        return showBaseForge(user, CLASS_LIST.get(0));
    }

    private List<PartialBotApiMethod<? extends Serializable>> craftItem(User user, String message) {
        var reply = createMessageTemplate(user);
        var message_mass = message.split("_");
        BaseItem item;
        if (user.getUserState()!=BotState.NONE){
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        if (message_mass.length!=2){
            reply.setText("Введите id одного предмета.");
            return List.of(reply);
        }
        try{
            item = baseItemRepository.findById(Long.parseLong(message_mass[1])).orElseThrow();
        }catch (Exception ignored){
            reply.setText("Нет такого предмета.");
            return List.of(reply);
        }
        var itemsToCraft= baseItemCraftRepository.findByCraftedBaseItemId(item);

        if (itemsToCraft.isEmpty()){
            reply.setText("Этот предмет нельзя создать.");
            return List.of(reply);
        }
        Map<IngameItem, Long> cells = new HashMap<>();
        for (BaseItemCraft baseItemCraft:
                itemsToCraft) {
            var cell = ingameItemRepository.findAllByUserAndBaseItem(user,baseItemCraft.getMaterialBaseItemId()).stream().filter(w->w.getItemsInStack()!=null && w.getItemsInStack() >= baseItemCraft.getCountOfMaterial()).findFirst().orElse(null);
            if (cell == null){
                reply.setText("У вас не хватает материалов.");
                return List.of(reply);
            }
            cells.put(cell,baseItemCraft.getCountOfMaterial());
        }

        for (IngameItem ingameItem :
                cells.keySet()) {
            if (ingameItem.getItemsInStack() == cells.get(ingameItem)){
                ingameItemRepository.delete(ingameItem);
            }else{
                ingameItem.setItemsInStack(ingameItem.getItemsInStack() - cells.get(ingameItem));
                ingameItemRepository.save(ingameItem);
            }
        }
        userGetItem(user,item);

        reply.setText(String.format("Вы успешно создали предмет *%s*",item.getName()));
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showBaseForge(User user, String message) {
        var reply = createMessageTemplate(user);
        reply.setText(getMessage(classRepository.findByName(message).orElseThrow()));
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();

        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Воин", CLASS_LIST.get(0)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Рейнджер", CLASS_LIST.get(1)));
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Маг", CLASS_LIST.get(2)));
        if (message.equals(CLASS_LIST.get(0))) {
            inlineKeyboardButtonsRowOne.remove(0);
        } else if (message.equals(CLASS_LIST.get(1))) {
            inlineKeyboardButtonsRowOne.remove(1);
        } else if (message.equals(CLASS_LIST.get(2))) {
            inlineKeyboardButtonsRowOne.remove(2);
        }
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return List.of(reply);
    }

    private String getMessage(Class baseClass) {
        List<Class> availableClasses = IngameUtil.getAvailableClasses(baseClass);
        List<BaseItem> baseItemList = new ArrayList<>();

        for (Class tmpClass :
                availableClasses) {
            baseItemList.addAll(baseItemCraftRepository.findDistinctByCraftedBaseItemId_ClassRequired(tmpClass).stream().map(BaseItemCraft::getCraftedBaseItemId).distinct().toList());
        }
        StringBuilder replyMessage = new StringBuilder();
        replyMessage.append(!baseItemList.isEmpty() ? String.format("Предметы для класса *%s* и его подклассов:%n%n", baseClass.getName()) : "Нет доступных предметов.");

        for (BaseItem base :
                baseItemList) {
            var materials = baseItemCraftRepository.findByCraftedBaseItemId(base).stream().toList();

            replyMessage.append(String.format(
                    "+%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s*%n_Минимальный требуемый класс - %s_%nТребуемые предметы:%n",
                    base.getDamage() == null ? 0 : base.getDamage(),
                    base.getArmor() == null ? 0 : base.getArmor(),
                    base.getName(),
                    base.getClassRequired().getName()));
            for (BaseItemCraft material :
                    materials) {
                replyMessage.append(String.format("x%d _%s_%n",material.getCountOfMaterial(),material.getMaterialBaseItemId().getName()));
            }
            replyMessage.append(String.format("Создать - /craft\\_%d%n%n", base.getId()));
        }
        return replyMessage.toString();
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.FORGE, Command.CRAFT);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return CLASS_LIST;
    }
}
