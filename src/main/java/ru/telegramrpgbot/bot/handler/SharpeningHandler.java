package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.ItemType;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;

import java.io.Serializable;
import java.util.*;

import static ru.telegramrpgbot.bot.util.IngameUtil.countItemArmor;
import static ru.telegramrpgbot.bot.util.IngameUtil.countItemDamage;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class SharpeningHandler implements Handler {
    private final String CALLBACK = "SHARPENING";
    private final IngameItemRepository ingameItemRepository;
    Random random = new Random();

    public SharpeningHandler(IngameItemRepository ingameItemRepository) {
        this.ingameItemRepository = ingameItemRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.toUpperCase().split("_")[0].equals("SHARPENING")) {
            return sharpenItem(user, message);
        }
        return showSharpeningMessage(user, message);
    }

    private List<PartialBotApiMethod<? extends Serializable>> sharpenItem(User user, String message) {
        var reply = createMessageTemplate(user);
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        IngameItem item;

        if (checker(user, messageList) != null) {
            reply.setText(Objects.requireNonNull(checker(user, messageList)));
            return List.of(reply);
        } else {
            item = ingameItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        }
        IngameItem sharpeningStones = ingameItemRepository.findAllByUserAndBaseItem_Type(user, ItemType.CONSUMABLE_SHARPENING_STONE).stream().findAny().orElse(null);

        if (sharpeningStones == null) {
            reply.setText("У вас нет _точильных камней_.");
            return List.of(reply);
        } else if (sharpeningStones.getItemsInStack() > 1) {
            sharpeningStones.setItemsInStack(sharpeningStones.getItemsInStack() - 1);
            ingameItemRepository.save(sharpeningStones);
            log.info(sharpeningStones.getItemsInStack()+"");
        } else if (sharpeningStones.getItemsInStack() == 1) {
            ingameItemRepository.delete(sharpeningStones);
        }
        if (isSuccessSharped(item)) {
            item.setSharpness(item.getSharpness() + 1);
            reply.setText("Заточка *успешна*. ✅ \n\n");
        } else {
            reply.setText("Заточка *провалилась*.❌ \n\n");
            item.setSharpness(item.getSharpness() - 1);
        }
        ingameItemRepository.save(item);
        SendMessage sendMessage = createSharpeningMessage(user, "/sharp_" + item.getId());
        sendMessage.setText(reply.getText() + sendMessage.getText());
        return List.of(sendMessage);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showSharpeningMessage(User user, String message) {
        return List.of(createSharpeningMessage(user, message));
    }

    private SendMessage createSharpeningMessage(User user, String message) {
        var reply = createMessageTemplate(user);
        List<String> messageList = Arrays.stream(message.split("_")).toList();
        IngameItem item;

        if (checker(user, messageList) != null) {
            reply.setText(Objects.requireNonNull(checker(user, messageList)));
            return reply;
        } else {
            item = ingameItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        }


        List<IngameItem> items = ingameItemRepository.findAllByUser(user);
        List<IngameItem> sharpeningStones = items.stream().filter(c -> c.getBaseItem().getType().name().contains("SHARPENING")).toList();
        if (sharpeningStones.isEmpty()) {
            reply.setText("У вас нет _точильных камней_.");
            return reply;
        }
        long sharpeningStonesCount = 0;
        for (IngameItem ingameItem :
                sharpeningStones) {
            sharpeningStonesCount += ingameItem.getItemsInStack();
        }
        double damage = countItemDamage(item);
        double armor = countItemArmor(item);

        StringBuilder replyMessage = new StringBuilder(String.format("Заточка предмета +%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s* (+%d)%n",
                Math.round(damage),
                Math.round(armor),
                item.getBaseItem().getName(),
                item.getSharpness()));

        replyMessage.append(String.format("Точильных камней - x%d%n", sharpeningStonesCount));
        replyMessage.append("\nШанс успешной заточки - %").append(sharpeningSuccess(item.getSharpness()+1)).append("\n");
        if (sharpeningSuccess(item.getSharpness() + 1) != 100) {
            replyMessage.append("\nПри неудаче уровень заточки предмета упадет.");
        }
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = new ArrayList<>();
        inlineKeyboardButtonsRowOne.add(createInlineKeyboardButton("Заточить \uD83D\uDD28", String.format("%s_%d", CALLBACK, item.getId())));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);

        reply.setText(replyMessage.toString());
        return reply;
    }

    private String checker(User user, List<String> messageList) {
        IngameItem item;
        if (user.getUserState() != BotState.NONE) {
            return ("Вы сейчас заняты!");
        }
        if (messageList.size() < 2 || messageList.get(1).length() < 1) {
            return ("Не указан id предмета");
        }
        try {
            item = ingameItemRepository.findAllById(Long.parseLong(messageList.get(1)));
        } catch (NumberFormatException exception) {
            return ("У вас нет таких предметов.");
        }
        if (!item.isEquipped()) {
            return ("Можно заточить только экипированный предмет.");
        }
        if (item.getSharpness() == 15) {
            return ("Предмет заточен максимально.");
        } else return null;

    }

    private int sharpeningSuccess(Long sharpness) {
        if (sharpness <= 5) {
            return 100;
        } else {
            return Math.round((1 / (float) sharpness) * 100 * 4);
        }
    }

    private boolean isSuccessSharped(IngameItem item) {
        var chance = sharpeningSuccess(item.getSharpness()+1);
        return random.nextInt(1, 101) <= chance;
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.SHARP);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(CALLBACK);
    }
}