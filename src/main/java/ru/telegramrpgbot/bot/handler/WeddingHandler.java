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
import ru.telegramrpgbot.bot.enums.BodyPart;
import ru.telegramrpgbot.bot.enums.MoveState;
import ru.telegramrpgbot.model.Fight;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.model.Move;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.repository.FightRepository;
import ru.telegramrpgbot.repository.MoveRepository;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class WeddingHandler implements Handler {
    private final UserRepository userRepository;

    public WeddingHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User actor, String message) {
        if (actor.getUserState() == BotState.WAITING_FOR_ANSWER) {
            if (message.equalsIgnoreCase(Command.CANCEL_PROPOSE.name())) {
                return cancel(actor);
            } else if (message.equalsIgnoreCase(Command.ACCEPT_PROPOSE.name())) {
                return accept(actor);
            }
            else {
                return waiting(actor);
            }
        }
        return propose(actor, message);
    }
    private List<PartialBotApiMethod<? extends Serializable>> propose(User actor, String message) {
        var messageToUser = createMessageTemplate(actor);
        String[] messageMass = message.split("_");
        if (messageMass.length < 2) {
            log.info("меньше 2");
            messageToUser.setText("Не указан ник игрока");
            return List.of(messageToUser);
        }
        if (messageMass.length > 2) {
            messageToUser.setText("Можно указать ник только одного игрока");
            return List.of(messageToUser);
        }
        if (userRepository.getUsersByName(messageMass[1]).orElse(null) == null) {
            messageToUser.setText("Нет такого игрока");
            return List.of(messageToUser);
        }
        if (messageMass[1].equalsIgnoreCase(actor.getName())) {

            messageToUser.setText("Нельзя сделать предложение себе(");
            return List.of(messageToUser);
        }
        User opponent = userRepository.getUsersByName(messageMass[1]).orElseThrow();
        if(opponent.getPartner() != null) {
            messageToUser.setText("У этого игрока уже есть пара.\uD83D\uDC94");
            return  List.of(messageToUser);
        }
        if(opponent.getUserState() != BotState.NONE) {
            messageToUser.setText("В данный момент этот игрок занят\nПопробуйте позже");
            return  List.of(messageToUser);
        }
        opponent.setPartner(actor);
        opponent.setUserState(BotState.WAITING_FOR_ANSWER);
        userRepository.save(opponent);
        var messageToOpponent = createMessageTemplate(opponent);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton(Command.CANCEL_PROPOSE.getRussian(), Command.CANCEL_PROPOSE.name()));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        messageToUser.setReplyMarkup(inlineKeyboardMarkup);
        messageToUser.setText("Ждем ответа.");

        InlineKeyboardMarkup inlineKeyboardMarkupOpponent = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOneOpponent = List.of(
                createInlineKeyboardButton(Command.ACCEPT_PROPOSE.getRussian(), Command.ACCEPT_PROPOSE.name()),
                createInlineKeyboardButton(Command.CANCEL_PROPOSE.getRussian(), Command.CANCEL_PROPOSE.name()));
        inlineKeyboardMarkupOpponent.setKeyboard(List.of(inlineKeyboardButtonsRowOneOpponent));

        messageToOpponent.setReplyMarkup(inlineKeyboardMarkupOpponent);
        messageToOpponent.setText(String.format("Вам сделал предложение *%s*!!! \uD83D\uDC8D", actor.getName()));
        log.info(messageToUser.getText());
        log.info(messageToOpponent.getText());
        return List.of(messageToUser, messageToOpponent);
    }
    private List<PartialBotApiMethod<? extends Serializable>> cancel(User actor) {
        User opponent = actor.getPartner();
        actor.setPartner(null);
        userRepository.save(actor);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText("К сожалению игрок ответил *нет*.\uD83D\uDC94");
        return  List.of(messageToOpponent);

    }
    private List<PartialBotApiMethod<? extends Serializable>> waiting(User actor) {
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText("Сначала ответьте на предложение .");
        return  List.of(messageToActor);

    }
    private List<PartialBotApiMethod<? extends Serializable>> accept(User actor) {
        actor.setUserState(BotState.NONE);
        User opponent = actor.getPartner();
        opponent.setPartner(actor);
        userRepository.save(actor);
        userRepository.save(opponent);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText(String.format("\uD83C\uDF7E УРА!!! _%s_ сказал ДА! ГОРЬКО!!! \uD83D\uDC9E", actor.getName()));
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText(String.format("Совет да любовь вам, _%s_ и _%s_!!! ГОРЬКО!!! \uD83C\uDF7E", actor.getName(), opponent.getName()));
        return List.of(messageToActor, messageToOpponent);

    }
    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.WAITING_FOR_ANSWER);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.PROPOSE);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(Command.ACCEPT_PROPOSE.name(), Command.CANCEL_PROPOSE.name());
    }
}