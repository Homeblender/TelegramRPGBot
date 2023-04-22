package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.Party;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.PartyRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class PartyHandler implements Handler {
//    public static final String NAME_ACCEPT = "/enter_name_accept";
//    public static final String NAME_CHANGE = "/WAITING_FOR_NAME";
//    public static final String NAME_CHANGE_CANCEL = "/enter_name_cancel";

    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    public static final String PARTY_NAME_ACCEPT = "/enter_party_name_accept";

    public PartyHandler(UserRepository userRepository, PartyRepository partyRepository) {
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        log.info(message);
        if (message.substring(1).equalsIgnoreCase(Command.EXIT.name())) {
            return exit(user);
        }
        if (message.equalsIgnoreCase(Command.CREATE_PARTY.name())) {
            var reply = createMessageTemplate(user);
            reply.setText("Введите название пати");
            user.setUserState(BotState.WAITING_FOR_PARTY_NAME);
            userRepository.save(user);
            return List.of(reply);
        }
        String[] messageMass = message.split("_");
        if ( messageMass[0].substring(1).equalsIgnoreCase(Command.INVITE.name())) {
            return invite(user, message);
        }
        if (message.equalsIgnoreCase(Command.ACCEPT_INVITE.name())) {
            return acceptInvite(user);
        }
        if (message.equalsIgnoreCase(Command.CANCEL_INVITE.name())) {
            return cancel(user);
        }
        if (message.equalsIgnoreCase(PARTY_NAME_ACCEPT)) {
            return accept(user);
        }
        if (user.getUserState() == BotState.WAITING_FOR_PARTY_NAME) {
            return create(user, message);
        }
        return party(user);
    }
    private List<PartialBotApiMethod<? extends Serializable>> party(User user) {
        if(user.getPartyId() == null) {
            var reply = createMessageTemplate(user);
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                    createInlineKeyboardButton(Command.CREATE_PARTY.getRussian(), Command.CREATE_PARTY.name()));
            inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
            reply.setReplyMarkup(inlineKeyboardMarkup);
            reply.setText("Вы не состоите ни в одной группе, но вы можете ее создать.");
            return List.of(reply);
        }
        var reply = createMessageTemplate(user);
        StringBuilder messageToUser = new StringBuilder("Группа " +
                user.getPartyId().getName() +
                "(Хост: " +
                userRepository.findUserByHostPartyId(user.getPartyId()).orElseThrow().getName() +
                "):");
        List<User> partyUsers = userRepository.findAllByPartyId(user.getPartyId());
        for (User partyUser : partyUsers) {
            messageToUser.append("\n   - ").append(partyUser.getName());
        }
        if (user.getHostPartyId() != null) {
            messageToUser.append("\n\nВы можете удалить команду - /exit");
        }
        else {
            messageToUser.append("\n\nВы можете выйти из группы - /exit");
        }
        reply.setText(messageToUser.toString());
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> create(User user, String message) {
        var reply = createMessageTemplate(user);
        if (partyRepository.getPartysByName(message).orElse(null) != null){
            reply.setText("Это название уже занято, выбери другое.");
            return List.of(reply);
        }
        if (!message.matches("[a-zA-Zа-яА-Я]+")){
            reply.setText("Введите название одним словом и только из букв.");
            return List.of(reply);
        }
        Party party;
        if(user.getHostPartyId() == null) {
            party = partyRepository.save(Party.builder().name(message).build());
        }
        else {
            party = user.getHostPartyId();
            party.setName(message);
            partyRepository.save(party);
        }
        user.setHostPartyId(party);
        user.setPartyId(party);
        userRepository.save(user);


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton("Да.", PARTY_NAME_ACCEPT));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));


        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText(String.format("Сохранить название: %s?", party.getName()));

        return List.of(reply);

    }
    private List<PartialBotApiMethod<? extends Serializable>> accept(User user) {
        log.info("0");
        user.setUserState(BotState.NONE);
        userRepository.save(user);
        var reply = createMessageTemplate(user);
        log.info("1");
        reply.setReplyMarkup(createBaseReplyKeyboard());
        reply.setText(String.format("Твоя группа называется: *%s*\n Ты можешь приглашать игроков в группу командой '/invite\\_<Имя игрока>'", user.getHostPartyId().getName()));
        log.info("2");

        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> invite(User user, String message) {
        if (user.getHostPartyId() == null) {
            var reply = createMessageTemplate(user);
            reply.setText("У вас нет группы.");
            return List.of(reply);
        }

        var messageToUser = createMessageTemplate(user);
        String[] messageMass = message.split("_");
        if (messageMass.length < 2) {
            messageToUser.setText("Не указано имя игрока.");
            return List.of(messageToUser);
        }
        if (messageMass.length > 2) {
            messageToUser.setText("Можно указать имя только одного игрока.");
            return List.of(messageToUser);
        }
        if (userRepository.getUsersByName(messageMass[1]).orElse(null) == null) {
            messageToUser.setText("Нет такого игрока.");
            return List.of(messageToUser);
        }
        if (messageMass[1].equalsIgnoreCase(user.getName())) {

            messageToUser.setText("Нельзя пригласить себя.");
            return List.of(messageToUser);
        }
        User opponent = userRepository.getUsersByName(messageMass[1]).orElseThrow();

        if(opponent.getPartyId() != null) {
            messageToUser.setText("Этот игрок уже состоит в пати.");
            return  List.of(messageToUser);
        }

        if(opponent.getUserState() != BotState.NONE) {
            messageToUser.setText("В данный момент этот игрок занят.\nПопробуйте позже.");
            return  List.of(messageToUser);
        }
        opponent.setPartyId(user.getHostPartyId());
        opponent.setUserState(BotState.WAITING_FOR_ANSWER_TO_INVITE);
        userRepository.save(opponent);
        var messageToOpponent = createMessageTemplate(opponent);

        messageToUser.setText("Ждем ответа.");

        InlineKeyboardMarkup inlineKeyboardMarkupOpponent = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOneOpponent = List.of(
                createInlineKeyboardButton(Command.ACCEPT_INVITE.getRussian(), Command.ACCEPT_INVITE.name()),
                createInlineKeyboardButton(Command.CANCEL_INVITE.getRussian(), Command.CANCEL_INVITE.name()));
        inlineKeyboardMarkupOpponent.setKeyboard(List.of(inlineKeyboardButtonsRowOneOpponent));

        messageToOpponent.setReplyMarkup(inlineKeyboardMarkupOpponent);
        messageToOpponent.setText(String.format("%s пригласил вас в пати %s", user.getName(), user.getHostPartyId().getName()));
        return List.of(messageToUser, messageToOpponent);
    }
    private List<PartialBotApiMethod<? extends Serializable>> cancel(User actor) {
        User opponent = userRepository.findUserByHostPartyId(actor.getPartyId()).orElseThrow();
        actor.setPartyId(null);
        if (actor.getUserState() == BotState.WAITING_FOR_ANSWER_TO_INVITE) {
            actor.setUserState(BotState.NONE);
        }
        userRepository.save(actor);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText(String.format("Игрок %s отменил приглашение.", actor.getName()));
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText("Вы отменили приглашение.");
        return  List.of(messageToOpponent, messageToActor);

    }
    private List<PartialBotApiMethod<? extends Serializable>> acceptInvite(User actor) {
        User opponent = userRepository.findUserByHostPartyId(actor.getPartyId()).orElseThrow();
        if (actor.getUserState() == BotState.WAITING_FOR_ANSWER_TO_INVITE) {
            actor.setUserState(BotState.NONE);
        }
        userRepository.save(actor);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText(String.format("Игрок %s принял приглашение.", actor.getName()));
        var messageToActor = createMessageTemplate(actor);
        messageToActor.setText("Вы приняли приглашение.");
        return  List.of(messageToOpponent, messageToActor);

    }
    private List<PartialBotApiMethod<? extends Serializable>> exit(User actor) {
        var messageToActor = createMessageTemplate(actor);
        if (actor.getPartyId() == null) {
            messageToActor.setText("Вы не состоите в группе");
            return List.of(messageToActor);
        }
        if (actor.getHostPartyId() != null) {
            List<User> partyUsers = userRepository.findAllByPartyId(actor.getHostPartyId());
            List<PartialBotApiMethod<? extends Serializable>> messages = new ArrayList<>();
            for (User user : partyUsers) {
                if (user.getHostPartyId() != null) {
                    user.setPartyId(null);
                    user.setHostPartyId(null);
                    userRepository.save(user);
                    var reply = createMessageTemplate(user);
                    reply.setText("Вы покинули группу. Комманды больше нет.");
                    messages.add(reply);
                }
                else {
                    user.setPartyId(null);
                    userRepository.save(user);
                    var reply = createMessageTemplate(user);
                    reply.setText("Хост покинул группу. Комманды больше нет.");
                    messages.add(reply);
                }
            }
            return messages;
        }
        User opponent = userRepository.findUserByHostPartyId(actor.getPartyId()).orElseThrow();
        log.info(opponent.getName());
        actor.setPartyId(null);
        userRepository.save(actor);
        var messageToOpponent = createMessageTemplate(opponent);
        messageToOpponent.setText(String.format("Игрок %s вышел из группы.", actor.getName()));

        messageToActor.setText("Вы вышли из группы.");
        return  List.of(messageToOpponent, messageToActor);

    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.WAITING_FOR_PARTY_NAME);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.PARTY, Command.INVITE, Command.EXIT);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(PARTY_NAME_ACCEPT, Command.CREATE_PARTY.name(), Command.CANCEL_INVITE.name(), Command.ACCEPT_INVITE.name());
    }
}
