package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.BodyPart;
import ru.telegramrpgbot.bot.enums.MoveState;
import ru.telegramrpgbot.model.*;
import ru.telegramrpgbot.bot.util.IngameUtil;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.repository.FightRepository;
import ru.telegramrpgbot.repository.MoveRepository;
import ru.telegramrpgbot.repository.ActiveSkillRepository;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class FightHandler implements Handler {
    private final UserRepository userRepository;
    private final FightRepository fightRepository;
    private final MoveRepository moveRepository;
    private final ActiveSkillRepository activeSkillRepository;
    private static final long waitingTime = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
    private static final long maxCountMoves = 14;

    public FightHandler(UserRepository userRepository, ActiveSkillRepository activeSkillRepository, FightRepository fightRepository, MoveRepository moveRepository) {
        this.userRepository = userRepository;
        this.fightRepository = fightRepository;
        this.moveRepository = moveRepository;
        this.activeSkillRepository = activeSkillRepository;
    }
    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User actor, String message) {
        log.info(message);
        log.info(actor.getUserState().name());
        if (actor.getUserState() == BotState.WAITING_FOR_OPPONENT || actor.getUserState() == BotState.WAITING_FOR_OPPONENT_MOVE) {
            if (message.equalsIgnoreCase(Command.CANCEL.name()) && actor.getUserState() == BotState.WAITING_FOR_OPPONENT) {
                return IngameUtil.cancel(actor);
            }
            else {
                return waiting(actor);
            }
        }

        if(actor.getUserState() == BotState.WAITING_FOR_MOVE) {
            log.info(actor.getName());
            return move(actor, message);
        }
        if (message.equalsIgnoreCase(Command.ACCEPT.name())) {
            return accept(actor);
        }
        return startFight(actor, message);
    }
    private List<PartialBotApiMethod<? extends Serializable>> startFight(User actor, String message) {
        log.info(actor.getName());
        log.info(message);
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
        if (messageMass[1].equalsIgnoreCase(actor.getName())){

            messageToUser.setText("Нельзя вызвать себя");
            return List.of(messageToUser);
        }
        User opponent = userRepository.getUsersByName(messageMass[1]).orElse(null);
        var messageToOpponent = createMessageTemplate(opponent);
        if(opponent.getUserState() != BotState.NONE) {
            messageToUser.setText("В данный момент этот игрок занят\nПопробуйте позже");
            return  List.of(messageToUser);
        }


        actor.setUserState(BotState.WAITING_FOR_OPPONENT);
        userRepository.save(actor);

        Fight fight = fightRepository.save(Fight.builder().user1Id(actor).user2Id(opponent).build());
        Move actorMove = moveRepository.getMoveByUserId(fight.getUser1Id()).orElse(null);
        Move opponentMove = moveRepository.getMoveByUserId(fight.getUser2Id()).orElse(null);
        if (actorMove != null) {
            moveRepository.delete(moveRepository.getMoveByUserId(fight.getUser1Id()).orElse(null));
        }
        if (opponentMove != null) {
            moveRepository.delete(moveRepository.getMoveByUserId(fight.getUser2Id()).orElse(null));
        }

        moveRepository.save(Move.builder().userId(actor).fightId(fight).endTime(new Timestamp(System.currentTimeMillis() + waitingTime)).build());
        moveRepository.save(Move.builder().userId(opponent).fightId(fight).endTime(new Timestamp(System.currentTimeMillis() + waitingTime)).build());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton(Command.CANCEL.getRussian(), Command.CANCEL.name()));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        messageToUser.setReplyMarkup(inlineKeyboardMarkup);
        messageToUser.setText("Ожидаем оппонента.");

        InlineKeyboardMarkup inlineKeyboardMarkupOpponent = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOneOpponent = List.of(
                createInlineKeyboardButton(Command.ACCEPT.getRussian(), Command.ACCEPT.name()),
                createInlineKeyboardButton(Command.CANCEL.getRussian(), Command.CANCEL.name()));
        inlineKeyboardMarkupOpponent.setKeyboard(List.of(inlineKeyboardButtonsRowOneOpponent));

        messageToOpponent.setReplyMarkup(inlineKeyboardMarkupOpponent);
        messageToOpponent.setText(String.format("Вам бросил вызов %s", actor.getName()));
        log.info(messageToUser.getText());
        log.info(messageToOpponent.getText());
        return List.of(messageToUser, messageToOpponent);
    }
    private List<PartialBotApiMethod<? extends Serializable>> waiting(User user) {
        var reply = createMessageTemplate(user);
        if (user.getUserState() == BotState.WAITING_FOR_OPPONENT_MOVE) {
            reply.setText("Ожидаем оппонента");
            return List.of(reply);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton(Command.CANCEL.getRussian(), Command.CANCEL.name()));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText("Ожидаем оппонента.");
        log.info(reply.getText());
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> accept(User opponent) {
        log.info("accept");
        Fight fight = moveRepository.getMoveByUserId(opponent).orElseThrow().getFightId();
        User actor = fight.getUser1Id();


        actor.setUserState(BotState.WAITING_FOR_MOVE);
        opponent.setUserState(BotState.WAITING_FOR_MOVE);
        userRepository.save(actor);
        userRepository.save(opponent);

        var messageToActor1 = createMessageTemplate(actor);
        var messageToOpponent1 = createMessageTemplate(opponent);
        String message1 = "Вызов принят!\nВыберите, что будете защищать\n";

        messageToActor1.setReplyMarkup(createFightDefenseKeyboard());
        messageToActor1.setText(message1);
        messageToOpponent1.setReplyMarkup(createFightDefenseKeyboard());
        messageToOpponent1.setText(message1);
        return List.of(messageToActor1, messageToOpponent1);
    }
    private List<PartialBotApiMethod<? extends Serializable>> move(User user, String message) {
        var reply = createMessageTemplate(user);

        Move move = moveRepository.getMoveByUserId(user).orElse(null);
        log.info("юзер мува " + move.getUserId().getName());
        log.info("юзер в хендлере "+user.getName());
        log.info("move");
        BodyPart part= Arrays.stream(BodyPart.values())
                .filter(h -> h.getTitle().equalsIgnoreCase(message))
                .findFirst()
                .orElse(null);
        log.info(part == null? "y":"n");
        var messageMas = message.split("_");
        if (part == null) {
            if (message.equalsIgnoreCase(Command.USE_ACTIVE_SKILL.getRussian())) {
                var activeSkills = activeSkillRepository.findAllByClassId(user.getUserClass());
                String messageToUser = "Мана: " + user.getCurrentMana() + "\nВаши навыки:";
                for (var activeSkill : activeSkills) {
                    messageToUser += "\n" + activeSkill.getName() +
                            "  " + activeSkill.getManaCost() + " маны  урон: х" + activeSkill.getDamageBonus() +
                            "  /skill\\_" + activeSkill.getId().toString();
                }
                reply.setText(messageToUser);
                return List.of(reply);
            }
            if (messageMas[0].substring(1).equalsIgnoreCase(Command.SKILL.name()) && move.getMoveState() == MoveState.DEFENSE_CHOSEN) {
                return useSkill(user, message);
            }
            reply.setText("Не верно введена часть тела");
            return List.of(reply);

        }

        if(move.getMoveState() == MoveState.NEW_MOVE) {
            log.info("new");
            log.info(part.getTitle());
            move.setDefense(part);
            move.setMoveState(MoveState.DEFENSE_CHOSEN);
            moveRepository.save(move);
            reply.setReplyMarkup(createFightAttackKeyboard());
            reply.setText("Теперь выберите часть тела для атаки");
            return List.of(reply);

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
        Fight fight = move.getFightId();
        User actor = fight.getUser1Id();
        User opponent = fight.getUser2Id();
        Move actorMove = moveRepository.getMoveByUserId(actor).orElse(null);
        Move opponentMove = moveRepository.getMoveByUserId(opponent).orElse(null);

        if (actorMove.getMoveState() == MoveState.MOVE_MADE &&
                opponentMove.getMoveState() == MoveState.MOVE_MADE) {
            long damageToActor = IngameUtil.countDamage(opponent);
            if (opponentMove.getActiveSkillId() != null) {
                actorMove.setHp(actorMove.getHp()
                        - Math.round((damageToActor - ((double)IngameUtil.countArmor(actor) * damageToActor)/100)
                        * opponentMove.getActiveSkillId().getDamageBonus()));
            }
            else {
                actorMove.setHp(actorMove.getHp()
                        - Math.round((damageToActor - ((double) IngameUtil.countArmor(actor) * damageToActor) / 100)
                        * (actorMove.getDefense() == opponentMove.getAttack() ? 0.5 : 1)));
            }

            long damageToOpponent = IngameUtil.countDamage(actor);
            if (actorMove.getActiveSkillId() != null) {
                opponentMove.setHp(opponentMove.getHp()
                        - Math.round((damageToOpponent - ((double)IngameUtil.countArmor(opponent) * damageToOpponent)/100)
                        * actorMove.getActiveSkillId().getDamageBonus()));
            }
            else {
                opponentMove.setHp(opponentMove.getHp()
                        - Math.round((damageToOpponent - ((double) IngameUtil.countArmor(opponent) * damageToOpponent) / 100)
                        * (opponentMove.getDefense() == actorMove.getAttack() ? 0.5 : 1)));
            }

            moveRepository.save(actorMove);
            moveRepository.save(opponentMove);

            var messageForActor = createMessageTemplate(actor);
            var messageForOpponent = createMessageTemplate(opponent);

            if (actorMove.getHp() <= 0 && opponentMove.getHp() > 0) {
                return ending(opponent, actor);
            }
            if (opponentMove.getHp() <= 0 && actorMove.getHp() > 0) {
                return ending(actor, opponent);
            }
            if (actorMove.getNum() > maxCountMoves || (opponentMove.getHp() <= 0 && actorMove.getHp() <= 0)) {
                actor.setUserState(BotState.NONE);
                opponent.setUserState(BotState.NONE);
                userRepository.save(actor);
                userRepository.save(opponent);

                moveRepository.delete(actorMove);
                moveRepository.delete(opponentMove);
                String message = "Ничья";
                if (actorMove.getNum() > maxCountMoves) {
                    message += "\nЗакончилось количество ходов";
                }
                fight.setFightState("Ничья");
                fightRepository.save(fight);
                messageForActor.setText(message);
                messageForOpponent.setText(message);
                var messages = createMessage(actor, opponent);
                var moveMessageForActor = messages[0];
                var moveMessageForOpponent = messages[1];

                return List.of(moveMessageForActor, messageForActor, moveMessageForOpponent, messageForOpponent);
            }
            var messages = createMessage(actor, opponent);
            messageForActor = messages[0];
            messageForOpponent = messages[1];
/*            String hitMessage = "%s\n" +
                    "Соперник защищал: %s\n\n" +
                    "%s\n" +
                    "Вы защищали: %s\n\n";
            String actorHitMessage;
            String actorAttack;
            String actorAttackToOpponent;
            if (actorMove.getActiveSkillId() != null) {
                actorAttack = "Использован навык " + actorMove.getActiveSkillId().getName();
                actorAttackToOpponent = "Соперник использовал навык " + actorMove.getActiveSkillId().getName();
            } else {
                actorAttack = "Вы били: " + actorMove.getAttack().getTitle();
                actorAttackToOpponent = "Соперник бил: " + actorMove.getAttack().getTitle();
            }
            String opponentAttack;
            String opponentAttackToActor;
            if (opponentMove.getActiveSkillId() != null) {
                opponentAttack = "Использован навык " + opponentMove.getActiveSkillId().getName();
                opponentAttackToActor = "Соперник использовал навык " + opponentMove.getActiveSkillId().getName();
            } else {
                opponentAttack = "Вы били: " + opponentMove.getAttack().getTitle();
                opponentAttackToActor = "Соперник бил: " + opponentMove.getAttack().getTitle();
            }
            actorHitMessage = String.format(hitMessage,
                    actorAttack,
                    opponentMove.getDefense().getTitle(),
                    opponentAttackToActor,
                    actorMove.getDefense().getTitle());
            String opponentHitMessage;
            opponentHitMessage = String.format(hitMessage,
                    opponentAttack,
                    actorMove.getDefense().getTitle(),
                    actorAttackToOpponent,
                    opponentMove.getDefense().getTitle());
            String message = "%sВаше HP: %s\n HP Соперника: %s\n\nСледующий ход №%s\nВыберете, что защищать";
            messageForActor.setReplyMarkup(createFightDefenseKeyboard());
            messageForOpponent.setReplyMarkup(createFightDefenseKeyboard());
            messageForActor.setText(String.format(message, actorHitMessage, actorMove.getHp(), opponentMove.getHp(), actorMove.getNum() + 2));
            messageForOpponent.setText(String.format(message, opponentHitMessage, opponentMove.getHp(), actorMove.getHp(), opponentMove.getNum() + 2));*/

            actor.setUserState(BotState.WAITING_FOR_MOVE);
            opponent.setUserState(BotState.WAITING_FOR_MOVE);
            userRepository.save(actor);
            userRepository.save(opponent);

            actorMove.setMoveState(MoveState.NEW_MOVE);
            opponentMove.setMoveState(MoveState.NEW_MOVE);
            actorMove.setDefense(null);
            actorMove.setAttack(null);
            actorMove.setActiveSkillId(null);
            opponentMove.setDefense(null);
            opponentMove.setAttack(null);
            opponentMove.setActiveSkillId(null);
            actorMove.setNum(actorMove.getNum() + 1);
            opponentMove.setNum(opponentMove.getNum() + 1);
            actorMove.setEndTime(new Timestamp(System.currentTimeMillis() + waitingTime));
            opponentMove.setEndTime(new Timestamp(System.currentTimeMillis() + waitingTime));
            moveRepository.save(actorMove);
            moveRepository.save(opponentMove);

            return List.of(messageForActor, messageForOpponent);
        } else {
            var reply = createMessageTemplate(user);
            reply.setText("Ожидаем оппонента");
            user.setUserState(BotState.WAITING_FOR_OPPONENT_MOVE);
            userRepository.save(user);
            return List.of(reply);
        }


    }
    private List<PartialBotApiMethod<? extends Serializable>> ending(User winner, User loser) {
        Move winnerMove = moveRepository.getMoveByUserId(winner).orElse(null);
        Fight fight = winnerMove.getFightId();
        fight.setFightState(String.format("Победил %s", winner.getName()));
        fightRepository.save(fight);


        var messages = createMessage(winner, loser);
        var moveMessageForWinner = messages[0];
        var moveMessageForLoser = messages[1];

        var messageForWinner = createMessageTemplate(winner);
        var messageForLoser = createMessageTemplate(loser);
        messageForWinner.setReplyMarkup(createBaseReplyKeyboard());
        messageForLoser.setReplyMarkup(createBaseReplyKeyboard());
        messageForWinner.setText("Вы победили!!!");
        messageForLoser.setText("Вы проиграли(");

        winner.setUserState(BotState.NONE);
        loser.setUserState(BotState.NONE);
        userRepository.save(winner);
        userRepository.save(loser);
        moveRepository.delete(winnerMove);
        moveRepository.delete(moveRepository.getMoveByUserId(loser).orElse(null));
        return List.of(moveMessageForWinner, messageForWinner, moveMessageForLoser, messageForLoser);
    }
    private List<PartialBotApiMethod<? extends Serializable>> useSkill(User user, String message) {
        var messageToUser = createMessageTemplate(user);
        String[] messageMass = message.split("_");
        if(messageMass.length<2){
            log.info("меньше 2");
            messageToUser.setText("Не указан id способности");
            return List.of(messageToUser);
        }
        if(messageMass.length>2){
            messageToUser.setText("Можно указать id только одной способности");
            return List.of(messageToUser);
        }
        if (activeSkillRepository.findById(Long.valueOf(messageMass[1])).orElse(null) == null){

            messageToUser.setText("У вас нет такого навыка");
            return List.of(messageToUser);
        }
        ActiveSkill skill = activeSkillRepository.findById(Long.valueOf(messageMass[1])).orElseThrow();
        if (user.getCurrentMana() < skill.getManaCost()) {
            messageToUser.setText("У вас недостаточно маны");
            return List.of(messageToUser);
        }
        Move move = moveRepository.getMoveByUserId(user).orElseThrow();
        move.setActiveSkillId(skill);
        move.setMoveState(MoveState.MOVE_MADE);
        moveRepository.save(move);
        log.info(Long.toString(skill.getManaCost()));
        log.info(Long.toString(user.getCurrentMana() - skill.getManaCost()));
        long mana = user.getCurrentMana() - skill.getManaCost();
        user.setCurrentMana(mana);
        userRepository.save(user);
        log.info(Long.toString(user.getCurrentMana()));

        return chek(user, move);
    }
    private SendMessage[] createMessage(User actor, User opponent) {
        Move actorMove = moveRepository.getMoveByUserId(actor).orElseThrow();
        Move opponentMove = moveRepository.getMoveByUserId(opponent).orElseThrow();
        var messageForActor = createMessageTemplate(actor);
        var messageForOpponent = createMessageTemplate(opponent);

        String hitMessage = "%s\n" +
                "Соперник защищал: %s\n\n" +
                "%s\n" +
                "Вы защищали: %s\n\n";
        String actorHitMessage;
        String actorAttack;
        String actorAttackToOpponent;
        if (actorMove.getActiveSkillId() != null) {
            actorAttack = "Использован навык " + actorMove.getActiveSkillId().getName();
            actorAttackToOpponent = "Соперник использовал навык " + actorMove.getActiveSkillId().getName();
        } else {
            actorAttack = "Вы били: " + actorMove.getAttack().getTitle();
            actorAttackToOpponent = "Соперник бил: " + actorMove.getAttack().getTitle();
        }
        String opponentAttack;
        String opponentAttackToActor;
        if (opponentMove.getActiveSkillId() != null) {
            opponentAttack = "Использован навык " + opponentMove.getActiveSkillId().getName();
            opponentAttackToActor = "Соперник использовал навык " + opponentMove.getActiveSkillId().getName();
        } else {
            opponentAttack = "Вы били: " + opponentMove.getAttack().getTitle();
            opponentAttackToActor = "Соперник бил: " + opponentMove.getAttack().getTitle();
        }
        actorHitMessage = String.format(hitMessage,
                actorAttack,
                opponentMove.getDefense().getTitle(),
                opponentAttackToActor,
                actorMove.getDefense().getTitle());
        String opponentHitMessage;
        opponentHitMessage = String.format(hitMessage,
                opponentAttack,
                actorMove.getDefense().getTitle(),
                actorAttackToOpponent,
                opponentMove.getDefense().getTitle());
        String message = "%sВаше HP: %s\n HP Соперника: %s\n\nСледующий ход №%s\nВыберете, что защищать";
        messageForActor.setReplyMarkup(createFightDefenseKeyboard());
        messageForOpponent.setReplyMarkup(createFightDefenseKeyboard());
        long actorHp;
        long opponentHp;
        if (actorMove.getHp() > 0) {
            actorHp = actorMove.getHp();
        } else {
            actorHp = 0;
        }
        if (opponentMove.getHp() > 0) {
            opponentHp = opponentMove.getHp();
        } else {
            opponentHp = 0;
        }
        messageForActor.setText(String.format(message, actorHitMessage, actorHp, opponentHp, actorMove.getNum() + 2));
        messageForOpponent.setText(String.format(message, opponentHitMessage, opponentHp, actorHp, opponentMove.getNum() + 2));

        return new SendMessage[]{messageForActor, messageForOpponent};
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
        return List.of(Command.ACCEPT.name(), Command.CANCEL.name());
    }
}
