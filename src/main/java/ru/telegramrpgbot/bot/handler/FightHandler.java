package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.BodyPart;
import ru.telegramrpgbot.model.Fight;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.model.Move;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.repository.FightRepository;
import ru.telegramrpgbot.repository.MoveRepository;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class FightHandler implements Handler {
    private final UserRepository userRepository;
    private final FightRepository fightRepository;
    private final MoveRepository moveRepository;

    public FightHandler(UserRepository userRepository, FightRepository fightRepository, MoveRepository moveRepository) {
        this.userRepository = userRepository;
        this.fightRepository = fightRepository;
        this.moveRepository = moveRepository;
    }
    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User actor, String message) {
        /*if (actor.getUserState() == BotState.WAITING_FOR_OPPONENT) {
            if (message.equalsIgnoreCase("/cancel")) {
                return cancel(actor);
            }
            else {
                return waiting(actor);
            }
        }*/
        if(actor.getUserState() == BotState.WAITING_FOR_MOVE) {
            return move(actor, message);
        }

        if (message.equalsIgnoreCase("/accept")) {
            return accept(actor);
        }
        var messageToUser = createMessageTemplate(actor);
        String[] messageMass = message.split("_");
        if(messageMass.length<2){
            log.info("меньше 2");
            messageToUser.setText("Не указан ник соперника");
            return List.of(messageToUser);
        }
        if(messageMass.length>2){
            messageToUser.setText("Можно указать ник только одного соперника");
            return List.of(messageToUser);
        }
        if (userRepository.getUsersByName(messageMass[1]).orElse(null) == null){

            messageToUser.setText("Нет такого игрока");
            return List.of(messageToUser);
        }
        User opponent = userRepository.getUsersByName(messageMass[1]).orElse(null);
        var messageToOpponent = createMessageTemplate(opponent);

        actor.setUserState(BotState.WAITING_FOR_OPPONENT);
        userRepository.save(actor);

        fightRepository.save(Fight.builder().user1Id(actor).user2Id(opponent).build());

        messageToUser.setText("Ожидаем оппонента");
        messageToOpponent.setText(String.format("Вам бросил вызов %s\nПринять вызов можно коммандой '/accept'", opponent.getName()));

        return List.of(messageToUser, messageToOpponent);
    }
    private List<PartialBotApiMethod<? extends Serializable>> cancel(User user) {
        user.setUserState(BotState.NONE);
        userRepository.save(user);
        var reply = createMessageTemplate(user);
        reply.setText("Вызов отменен");
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> waiting(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Ожидаем оппонента.\nВы можете отменить вызов командой '/cancel'");
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> accept(User opponent) {
        Fight fight = fightRepository.getFightByUser2Id(opponent).orElse(null);
        User actor = fight.getUser1Id();
        actor.setUserState(BotState.WAITING_FOR_MOVE);
        opponent.setUserState(BotState.WAITING_FOR_MOVE);
        userRepository.save(actor);
        userRepository.save(opponent);

        var messageToActor1 = createMessageTemplate(actor);
        var messageToOpponent1 = createMessageTemplate(opponent);
        var messageToActor2 = createMessageTemplate(actor);
        var messageToOpponent2 = createMessageTemplate(opponent);
        String message1 = "Вызов принят!";
        String message2 = "Выберите, что будете защищать\n(Голова, грудь, ноги)";
        messageToActor1.setText(message1);
        messageToActor2.setText(message1);
        messageToActor1.setText(message2);
        messageToActor2.setText(message2);
        return List.of(messageToActor1, messageToActor2, messageToOpponent1, messageToOpponent2);
    }
    private List<PartialBotApiMethod<? extends Serializable>> move(User user, String message) {
        var reply = createMessageTemplate(user);

        Move move = moveRepository.getMoveByUserId(user).orElseGet(() ->
                moveRepository.save(Move.builder().userId(user).build()));
        BodyPart part= Arrays.stream(BodyPart.values())
                .filter(h -> h.getTitle().equalsIgnoreCase(message))
                .findFirst()
                .orElse(null);

        if (part == null) {
            reply.setText("Не верно введена часть тела");
            return List.of(reply);
        }

        if (move.getAttack() != null) {
            move.setDefense(part);
            move.setAttack(null);
            move.setNum(move.getNum()+1);
            moveRepository.save(move);
            reply.setText("Теперь выберите часть тела для атаки");
        }
        else {
            if (move.getDefense() == null) {
                move.setDefense(part);
                moveRepository.save(move);
                reply.setText("Теперь выберите часть тела для атаки");
            }
            else {
                move.setAttack(part);
                moveRepository.save(move);
                reply.setText("Следующий ход");
            }
        }
        return List.of(reply);
    }
    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.WAITING_FOR_OPPONENT, BotState.WAITING_FOR_MOVE);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.FIGHT);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
