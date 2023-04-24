package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.IngameItem;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.IngameItemRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.telegramrpgbot.bot.util.IngameUtil.*;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class TradeHandler implements Handler {
    private final UserRepository userRepository;
    private final IngameItemRepository ingameItemRepository;
    private final List<String> activeOffers = new ArrayList<>();

    public TradeHandler(UserRepository userRepository, IngameItemRepository ingameItemRepository) {
        this.userRepository = userRepository;
        this.ingameItemRepository = ingameItemRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).toUpperCase().split("_")[0].equals(Command.TRADE.name())) {
            return tradeStart(user, message);
        } else if (activeOffers.contains(message) && message.contains("TRADEACCEPT")) {
            return tradeAccept(user, message);
        }else if (activeOffers.contains(message) && message.contains("TRADEDENIAL")) {
            return tradeDenial(user, message);
        }
        return tradeIsNotValid(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> tradeDenial(User user, String message) {
        var reply = createMessageTemplate(user);
        String[] messageMass = message.split("_");
        User secondUser = userRepository.findById(Long.parseLong(messageMass[2])).orElseThrow();
        Remover(message);
        reply.setText("Вы отказались покупать предмет.");
        var messageToSecondUser = createMessageTemplate(secondUser);
        messageToSecondUser.setText(String.format("Игрок [%s](tg://user?id=%d) отказался покупать предмет.",user.getName(),user.getChatId()));
        return List.of(reply, messageToSecondUser);
    }

    private List<PartialBotApiMethod<? extends Serializable>> tradeAccept(User user, String message) {
        var reply = createMessageTemplate(user);
        Remover(message);
        String[] messageMass = message.split("_");
        User secondUser = userRepository.findById(Long.parseLong(messageMass[2])).orElseThrow();
        IngameItem item;
        var price = Long.parseLong(messageMass[4]);
        if (user.getUserState() != BotState.NONE) {
            return tradeIsNotValid(user);
        }
        if (user.getChatId() != Long.parseLong(messageMass[3])) {
            return tradeIsNotValid(user);
        }
        if (user.getGold() < price) {
            reply.setText("У вас недостаточно денег.");
            return List.of(reply);
        }
        try {
            item = ingameItemRepository.findAllByIdAndUser(Long.parseLong(messageMass[1]), secondUser);
            if (item.isEquipped()) {
                return tradeIsNotValid(user);
            }
        } catch (Exception exception) {
            return tradeIsNotValid(user);
        }

        if (item.getItemsInStack() == null){
            item.setUser(user);
        }else if(item.getItemsInStack() == 1){
            item.setUser(user);
        }else{
            userGetItem(user,item.getBaseItem());
            item.setItemsInStack(item.getItemsInStack()-1);
        }

        user.setGold(user.getGold()-price);
        secondUser.setGold(secondUser.getGold()+price);
        userRepository.saveAll(List.of(user,secondUser));
        ingameItemRepository.save(item);
        reply.setText("Вы успешно купили предмет.");
        var messageToSecondUser = createMessageTemplate(secondUser);
        messageToSecondUser.setText(String.format("Игрок [%s](tg://user?id=%d) купил ваш предмет.",user.getName(),user.getChatId()));
        return List.of(reply, messageToSecondUser);
    }
    private void Remover(String message){
        var temp = activeOffers.stream().filter(w-> w.equals(message.replace("TRADEACCEPT", "TRADEDENIAL"))
                || w.equals(message.replace("TRADEDENIAL", "TRADEACCEPT"))).toList();
        activeOffers.removeAll(temp);
    }

    private List<PartialBotApiMethod<? extends Serializable>> tradeIsNotValid(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Предожение не актуально.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> tradeStart(User user, String message) {
        var reply = createMessageTemplate(user);
        User secondUser;
        IngameItem item;
        long price;

        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }

        String[] messageMass = message.split("_");
        if (messageMass.length < 4) {
            reply.setText("Введите игрока id предмета и его цену.");
        }
        try {
            secondUser = userRepository.findAllByName(messageMass[1]).orElseThrow();
        } catch (NumberFormatException exception) {
            reply.setText("Нет такого игрока.");
            return List.of(reply);
        }

        if (secondUser.getUserState() != BotState.NONE) {
            reply.setText("Игрок сейчас занят.");
            return List.of(reply);
        }
        try {
            item = ingameItemRepository.findAllByIdAndUser(Long.parseLong(messageMass[2]), user);
            if (item.isEquipped()) {
                reply.setText("Нельзя продать экипированный предмет.");
                return List.of(reply);
            }
        } catch (Exception exception) {
            reply.setText("У вас не такого предмета.");
            return List.of(reply);
        }
        try {
            price = Long.parseLong(messageMass[3]);
        } catch (Exception exception) {
            reply.setText("Цена должна быть целым числом.");
            return List.of(reply);
        }
        if (price < 1) {
            reply.setText("Цена должна быть больше нуля.");
            return List.of(reply);
        }


        var messageToSecondUser = createMessageTemplate(secondUser);
        double damage = countItemDamage(item);
        double armor = countItemArmor(item);
        long sharpness = item.getSharpness() == null?0:item.getSharpness();

        messageToSecondUser.setText(String.format(
                "Игрок [%s](tg://user?id=%d) предлагает вам купить предмет.%n%n+%d\uD83D\uDDE1 +%d\uD83D\uDEE1 *%s* (+%d)%n%n Цена - %d \uD83D\uDCB0",
                user.getName(),
                user.getChatId(),
                Math.round(damage),
                Math.round(armor),
                item.getBaseItem().getName(),
                sharpness,
                price
        ));
        String accept = String.format("TRADEACCEPT_%d_%d_%d_%d", item.getId(), user.getChatId(), secondUser.getChatId(), price);
        String denial = String.format("TRADEDENIAL_%d_%d_%d_%d", item.getId(), user.getChatId(), secondUser.getChatId(), price);
        activeOffers.add(accept);
        activeOffers.add(denial);
        InlineKeyboardMarkup inlineKeyboardMarkupOpponent = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOneOpponent = List.of(
                createInlineKeyboardButton("Купить", accept),
                createInlineKeyboardButton("Отказаться", denial));
        inlineKeyboardMarkupOpponent.setKeyboard(List.of(inlineKeyboardButtonsRowOneOpponent));

        messageToSecondUser.setReplyMarkup(inlineKeyboardMarkupOpponent);
        reply.setText("Предложение отправлено.");


        return List.of(messageToSecondUser,reply);
    }


    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.TRADE);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return activeOffers;
    }
}
