package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
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
        return startFight(actor, message);
    }
    private List<PartialBotApiMethod<? extends Serializable>> startFight(User actor, String message) {
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

        Fight fight = moveRepository.getMoveByUserId(user).get().getFightId();
        moveRepository.delete(moveRepository.getMoveByUserId(fight.getUser1Id()).orElse(null));
        moveRepository.delete(moveRepository.getMoveByUserId(fight.getUser2Id()).orElse(null));
        fightRepository.delete(fight);

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
        moveRepository.save(Move.builder().userId(actor).fightId(fight).build());
        moveRepository.save(Move.builder().userId(opponent).fightId(fight).build());
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

        Move move = moveRepository.getMoveByUserId(user).orElse(null);
        log.info("move");
        BodyPart part= Arrays.stream(BodyPart.values())
                .filter(h -> h.getTitle().equalsIgnoreCase(message))
                .findFirst()
                .orElse(null);
        log.info(part == null? "y":"n");
        if (part == null) {
            reply.setText("Не верно введена часть тела");
            return List.of(reply);
        }

        if(move.getMoveState() == MoveState.NEW_MOVE) {
            log.info("new");
            log.info(part.getTitle());
            move.setDefense(part);
            move.setMoveState(MoveState.DEFENSE_CHOSEN);
            moveRepository.save(move);
            reply.setText("Теперь выберите часть тела для атаки");

        }
        if (move.getMoveState() == MoveState.DEFENSE_CHOSEN) {
            move.setAttack(part);
            move.setMoveState(MoveState.MOVE_MADE);
            moveRepository.save(move);

            return chek(user, move);
        }
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> chek(User user, Move move) {
        User actor = move.getFightId().getUser1Id();
        User opponent = move.getFightId().getUser2Id();
        Move actorMove = moveRepository.getMoveByUserId(actor).orElse(null);
        Move opponentMove = moveRepository.getMoveByUserId(opponent).orElse(null);

        if (actorMove.getMoveState() == MoveState.MOVE_MADE &&
                opponentMove.getMoveState() == MoveState.MOVE_MADE) {
            long damageToActor = IngameUtil.countDamage(opponent);
            actorMove.setHp(actorMove.getHp()
                    - Math.round((damageToActor - ((double)IngameUtil.countArmor(actor) * damageToActor)/100)
                    * (actorMove.getDefense() == opponentMove.getAttack()? 0.5 : 1)));

            long damageToOpponent = IngameUtil.countDamage(actor);
            opponentMove.setHp(opponentMove.getHp()
                    - Math.round((damageToOpponent - ((double)IngameUtil.countArmor(opponent) * damageToOpponent)/100)
                    * (opponentMove.getDefense() == actorMove.getAttack()? 0.5 : 1)));

            moveRepository.save(actorMove);
            moveRepository.save(opponentMove);

            var messageForActor = createMessageTemplate(actor);
            var messageForOpponent = createMessageTemplate(opponent);

            if (actorMove.getHp() < 0) {
                return ending(opponent, actor);
            }
            if (opponentMove.getHp() < 0) {
                return ending(actor, opponent);
            }
            if (actorMove.getNum() > 9) {
                moveRepository.delete(actorMove);
                moveRepository.delete(opponentMove);
                String message = "Ничья\nЗвкончилось количество ходов";
                messageForActor.setText(message);
                messageForOpponent.setText(message);
                return List.of(messageForActor, messageForOpponent);
            }

            String message = "Следующий ход\nВыберете, что защищать";
            messageForActor.setText(message);
            messageForOpponent.setText(message);

            actor.setUserState(BotState.WAITING_FOR_MOVE);
            opponent.setUserState(BotState.WAITING_FOR_MOVE);
            userRepository.save(actor);
            userRepository.save(opponent);

            actorMove.setMoveState(MoveState.NEW_MOVE);
            opponentMove.setMoveState(MoveState.NEW_MOVE);
            actorMove.setDefense(null);
            actorMove.setAttack(null);
            opponentMove.setDefense(null);
            opponentMove.setAttack(null);
            actorMove.setNum(actorMove.getNum() + 1);
            opponentMove.setNum(opponentMove.getNum() + 1);
            moveRepository.save(actorMove);
            moveRepository.save(opponentMove);

            return List.of(messageForActor, messageForOpponent);
        } else {
            var reply = createMessageTemplate(user);
            reply.setText("Ожидаем оппонента.\nВы можете отменить вызов командой '/cancel'");
            user.setUserState(BotState.WAITING_FOR_OPPONENT);
            userRepository.save(user);
            return List.of(reply);
        }


    }
    private List<PartialBotApiMethod<? extends Serializable>> ending(User winner, User loser) {
        Move winnerMove = moveRepository.getMoveByUserId(winner).orElse(null);
        winnerMove.getFightId().setFightState(String.format("Победил %s", winner.getName()));

        moveRepository.delete(winnerMove);
        moveRepository.delete(moveRepository.getMoveByUserId(loser).orElse(null));

        var messageForWinner = createMessageTemplate(winner);
        var messageForLoser = createMessageTemplate(loser);
        messageForWinner.setText("Вы победили!!!");
        messageForLoser.setText("Вы проиграли(");
        return List.of(messageForWinner, messageForLoser);
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
