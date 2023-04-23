package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static ru.telegramrpgbot.bot.util.IngameUtil.Announcement;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createInlineKeyboardButton;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class WeddingHandler implements Handler {
    private final UserRepository userRepository;

    public WeddingHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User actor, String message) {
        if (message.equalsIgnoreCase(Command.CANCEL_PROPOSE.name())) {
            return cancel(actor);
        }
        if (actor.getUserState() == BotState.WAITING_FOR_ANSWER) {
            if (message.equalsIgnoreCase(Command.ACCEPT_PROPOSE.name())) {
                return accept(actor);
            }
            else {
                return waiting(actor);
            }
        }
        log.info(message);
        if (message.substring(1).equalsIgnoreCase(Command.DIVORCE.name())) {
            return divorce(actor);
        }
        return propose(actor, message);
    }
    private List<PartialBotApiMethod<? extends Serializable>> propose(User actor, String message) {
        var messageToUser = createMessageTemplate(actor);
        String[] messageMass = message.split("_");
        if(actor.getPartner() != null) {
            messageToUser.setText("У вас уже есть пара.\uD83D\uDC94\n" +
                    "Если вы хотите сыграть свадьбу с кем-то еще, сначала разведитесь с нынешним партнером.");
            return  List.of(messageToUser);
        }
        User partner = userRepository.findAll().stream()
                .filter(h -> h.getPartner() != null)
                .filter(h -> Objects.equals(h.getPartner().getChatId(), actor.getChatId()))
                .findFirst()
                .orElse(null);
        if(partner != null) {
            messageToUser.setText(String.format("Вы уже сделали предложение %s.\uD83D\uDC94\n", partner.getName()));
            return  List.of(messageToUser);
        }
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
        return List.of(messageToUser, messageToOpponent);
    }
    private List<PartialBotApiMethod<? extends Serializable>> cancel(User actor) {
        User opponent = actor.getPartner();
        if (opponent == null) {
            opponent = userRepository.findAll().stream()
                    .filter(h -> h.getPartner() != null)
                    .filter(h -> Objects.equals(h.getPartner().getChatId(), actor.getChatId()))
                    .findFirst()
                    .orElse(null);
        }
        actor.setPartner(null);
        opponent.setPartner(null);
        if (actor.getUserState() == BotState.WAITING_FOR_ANSWER) {
            actor.setUserState(BotState.NONE);
        }
        if (opponent.getUserState() == BotState.WAITING_FOR_ANSWER) {
            opponent.setUserState(BotState.NONE);
        }
        userRepository.save(actor);
        userRepository.save(opponent);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText("К сожалению, игрок отменил предложение.\uD83D\uDC94");
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText("Вы отменили предложение.\uD83D\uDC94");
        return  List.of(messageToOpponent, messageToActor);

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
        var announcement = String.format("Игрок [%s](tg://user?id=%d) и Игрок [%s](tg://user?id=%d) поженились!\uD83D\uDC9E\nГОРЬКО!!!\uD83C\uDF7E",
                actor.getName(), actor.getChatId(), opponent.getName(), opponent.getChatId());
        var sendMessageList = Announcement(announcement);
        sendMessageList.add(messageToActor);
        sendMessageList.add(messageToOpponent);

        return sendMessageList;

    }
    private List<PartialBotApiMethod<? extends Serializable>> divorce(User actor) {
        if (actor.getPartner() == null) {
            var messageToActor = createMessageTemplate(actor);
            messageToActor.setText("У вас нет партнера, чтобы с ним развестись");
            return List.of(messageToActor);
        }
        User opponent = actor.getPartner();
        opponent.setPartner(null);
        actor.setPartner(null);
        userRepository.save(actor);
        userRepository.save(opponent);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText(String.format("К сожалению _%s_ разорвал ваш брак.\nВы разведены...\uD83D\uDC94", actor.getName()));
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText(String.format("Вы больше не вместе с _%s_.\nВаш брак рассторгнут.\uD83D\uDC94", opponent.getName()));
        var announcement = String.format("Игрок [%s](tg://user?id=%d) и Игрок [%s](tg://user?id=%d) развелись\uD83D\uDC94",
                actor.getName(), actor.getChatId(), opponent.getName(), opponent.getChatId());
        var sendMessageList = Announcement(announcement);
        sendMessageList.add(messageToActor);
        sendMessageList.add(messageToOpponent);
        return sendMessageList;

    }
    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.WAITING_FOR_ANSWER);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.PROPOSE, Command.DIVORCE);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(Command.ACCEPT_PROPOSE.name(), Command.CANCEL_PROPOSE.name());
    }
}