package ru.telegramrpgbot.bot.handler;


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
public class FightHandler implements Handler {
    private final UserRepository userRepository;
    private final FightRepository fightRepository;
    private final MoveRepository moveRepository;
    private final ActiveSkillRepository activeSkillRepository;
    private static final long waitingTime = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
    private static final long maxCountMoves = 40L;
    private static final float defenseCoef = 0.5f;
    private static final float healthCoef = 0.3f;

    public FightHandler(UserRepository userRepository, ActiveSkillRepository activeSkillRepository, FightRepository fightRepository, MoveRepository moveRepository) {
        this.userRepository = userRepository;
        this.fightRepository = fightRepository;
        this.moveRepository = moveRepository;
        this.activeSkillRepository = activeSkillRepository;
    }
    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User actor, String message) {
        if (actor.getUserState() == BotState.WAITING_FOR_OPPONENT || actor.getUserState() == BotState.WAITING_FOR_OPPONENT_MOVE) {
            if (message.equalsIgnoreCase(Command.CANCEL.name()) && actor.getUserState() == BotState.WAITING_FOR_OPPONENT) {
                return IngameUtil.cancel(actor);
            }
            else {
                return waiting(actor);
            }
        }

        if(actor.getUserState() == BotState.WAITING_FOR_MOVE) {
            return move(actor, message);
        }
        if (message.equalsIgnoreCase(Command.ACCEPT.name())) {
            return accept(actor);
        }
        return startFight(actor, message);
    }
    private List<PartialBotApiMethod<? extends Serializable>> startFight(User actor, String message) {
        var messageToUser = createMessageTemplate(actor);
        String[] messageMass = message.split("_");
        if(messageMass.length != 3){
            messageToUser.setText("Команда должна выглядеть как /fight\\_<Ник соперника>\\_<ставка на бой>");
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
        if (!messageMass[2].matches("[0-9]+")) {
            messageToUser.setText("Ставка указана некорректно");
            return List.of(messageToUser);
        }
        long bet = Long.valueOf(messageMass[2]);
        if (bet > actor.getGold()) {
            messageToUser.setText("У вас недостаточно денег для такой ставки");
            return List.of(messageToUser);
        }
        User opponent = userRepository.getUsersByName(messageMass[1]).orElseThrow();
        if (bet > opponent.getGold()) {
            messageToUser.setText("У этого игрока недостаточно денег для такой ставки");
            return List.of(messageToUser);
        }
        if (IngameUtil.countDamage(actor) <= 0) {
            messageToUser.setText("У вас не экипировано оружие");
            return List.of(messageToUser);
        }
        if (IngameUtil.countDamage(opponent) <= 0) {
            messageToUser.setText("У этого игрока не экипировано оружие");
            return List.of(messageToUser);
        }
        var messageToOpponent = createMessageTemplate(opponent);
        if(opponent.getUserState() != BotState.NONE) {
            messageToUser.setText("В данный момент этот игрок занят\nПопробуйте позже");
            return  List.of(messageToUser);
        }


        actor.setUserState(BotState.WAITING_FOR_OPPONENT);
        userRepository.save(actor);

        Fight fight = fightRepository.save(Fight.builder().user1Id(actor).user2Id(opponent).bet(bet).build());
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
        messageToUser.setText("Ожидаем оппонента.\uD83D\uDD53");

        InlineKeyboardMarkup inlineKeyboardMarkupOpponent = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOneOpponent = List.of(
                createInlineKeyboardButton(Command.ACCEPT.getRussian(), Command.ACCEPT.name()),
                createInlineKeyboardButton(Command.CANCEL.getRussian(), Command.CANCEL.name()));
        inlineKeyboardMarkupOpponent.setKeyboard(List.of(inlineKeyboardButtonsRowOneOpponent));

        messageToOpponent.setReplyMarkup(inlineKeyboardMarkupOpponent);
        messageToOpponent.setText(String.format("\uD83D\uDC4AВам бросил вызов %s level: %d\nСтавка %d\uD83D\uDCB0", actor.getName(), actor.getLevel(), fight.getBet()));

        return List.of(messageToUser, messageToOpponent);
    }

    private List<PartialBotApiMethod<? extends Serializable>> waiting(User user) {
        var reply = createMessageTemplate(user);
        if (user.getUserState() == BotState.WAITING_FOR_OPPONENT_MOVE) {
            reply.setText("Ожидаем оппонента\uD83D\uDD53");
            return List.of(reply);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton(Command.CANCEL.getRussian(), Command.CANCEL.name()));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText("Ожидаем оппонента\uD83D\uDD53");
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> accept(User opponent) {
        Fight fight = moveRepository.getMoveByUserId(opponent).orElseThrow().getFightId();
        User actor = fight.getUser1Id();


        actor.setUserState(BotState.WAITING_FOR_MOVE);
        opponent.setUserState(BotState.WAITING_FOR_MOVE);
        userRepository.save(actor);
        userRepository.save(opponent);

        var messageToActor1 = createMessageTemplate(actor);
        var messageToOpponent1 = createMessageTemplate(opponent);
        String message1 = "\uD83D\uDC4AВызов принят!\nВыберите, что будете защищать\uD83D\uDEE1\n";

        messageToActor1.setReplyMarkup(createFightDefenseKeyboard());
        messageToActor1.setText(message1);
        messageToOpponent1.setReplyMarkup(createFightDefenseKeyboard());
        messageToOpponent1.setText(message1);
        return List.of(messageToActor1, messageToOpponent1);
    }
    private List<PartialBotApiMethod<? extends Serializable>> move(User user, String message) {
        var reply = createMessageTemplate(user);

        Move move = moveRepository.getMoveByUserId(user).orElse(null);
        BodyPart part= Arrays.stream(BodyPart.values())
                .filter(h -> h.getTitle().equalsIgnoreCase(message))
                .findFirst()
                .orElse(null);
        var messageMas = message.split("_");
        if (part == null) {
            if (message.equalsIgnoreCase(Command.USE_ACTIVE_SKILL.getRussian())) {
                var activeSkills = activeSkillRepository.findAllByClassId(user.getUserClass());
                StringBuilder messageToUser = new StringBuilder("\uD83D\uDD39 Мана: " + user.getCurrentMana() + "\nВаши навыки:");
                for (var activeSkill : activeSkills) {
                    messageToUser.append("\n").append(activeSkill.getName()).append("  ").append(activeSkill.getManaCost()).append(" маны  урон: х").append(activeSkill.getDamageBonus()).append("  /skill\\_").append(activeSkill.getId().toString());
                }
                reply.setText(messageToUser.toString());
                return List.of(reply);
            }
            if (messageMas[0].substring(1).equalsIgnoreCase(Command.SKILL.name()) && move.getMoveState() == MoveState.DEFENSE_CHOSEN) {
                return useSkill(user, message);
            }
            reply.setText("Не верно введена часть тела");
            return List.of(reply);

        }

        if(move.getMoveState() == MoveState.NEW_MOVE) {
            move.setDefense(part);
            move.setMoveState(MoveState.DEFENSE_CHOSEN);
            moveRepository.save(move);
            reply.setReplyMarkup(createFightAttackKeyboard());
            reply.setText("Теперь выберите часть тела для атаки\uD83D\uDDE1");
            return List.of(reply);

        }
        if (move.getMoveState() == MoveState.DEFENSE_CHOSEN) {
            move.setAttack(part);
            move.setMoveState(MoveState.MOVE_MADE);
            moveRepository.save(move);

            return check(user);
        }
        return List.of(reply);
    }
    private List<PartialBotApiMethod<? extends Serializable>> check(User user) {

        Fight fight = moveRepository.getMoveByUserId(user).orElseThrow().getFightId();
        User actor = fight.getUser1Id();
        User opponent = fight.getUser2Id();
        Move actorMove = moveRepository.getMoveByUserId(actor).orElseThrow();
        Move opponentMove = moveRepository.getMoveByUserId(opponent).orElseThrow();

        if (actorMove.getMoveState() == MoveState.MOVE_MADE &&
                opponentMove.getMoveState() == MoveState.MOVE_MADE) {

            long actorArm = IngameUtil.countArmor(actor);
            long baseDamageToActor = IngameUtil.countDamage(opponent);
            long damageToActor;
            if (opponentMove.getActiveSkillId() != null) {
                damageToActor = (long) (baseDamageToActor * opponentMove.getActiveSkillId().getDamageBonus());
            } else if (actorMove.getDefense() == opponentMove.getAttack()) {
                damageToActor = (long) (baseDamageToActor * defenseCoef);
            } else {
                damageToActor = baseDamageToActor;
            }
            long finalDamageToActor = IngameUtil.damageTakes(actorArm, damageToActor);
            long actorHp = actorMove.getHp() - finalDamageToActor;

            long opponentArm = IngameUtil.countArmor(opponent);
            long baseDamageToOpponent = IngameUtil.countDamage(actor);
            long damageToOpponent;
            if (actorMove.getActiveSkillId() != null) {
                damageToOpponent = (long) (baseDamageToOpponent * actorMove.getActiveSkillId().getDamageBonus());
            } else if (opponentMove.getDefense() == actorMove.getAttack()) {
                damageToOpponent = (long) (baseDamageToOpponent * defenseCoef);
            } else {
                damageToOpponent = baseDamageToOpponent;
            }
            long finalDamageToOpponent = IngameUtil.damageTakes(opponentArm, damageToOpponent);
            long opponentHp = opponentMove.getHp() - finalDamageToOpponent;

            actorMove.setHp(actorHp);
            opponentMove.setHp(opponentHp);
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

                moveRepository.delete(actorMove);
                moveRepository.delete(opponentMove);

                return List.of(moveMessageForActor, messageForActor, moveMessageForOpponent, messageForOpponent);
            }
            var messages = createMessage(actor, opponent);
            var moveMessageForActor = messages[0];
            var moveMessageForOpponent = messages[1];
            messageForOpponent.setText(String.format("Следующий ход №%s\nВыберете, что защищать🛡", opponentMove.getNum() + 2));
            messageForActor.setText(String.format("Следующий ход №%s\nВыберете, что защищать🛡", actorMove.getNum() + 2));
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

            return List.of(moveMessageForActor, moveMessageForOpponent, messageForActor, messageForOpponent);
        } else {
            var reply = createMessageTemplate(user);
            reply.setText("Ожидаем оппонента\uD83D\uDD53");
            user.setUserState(BotState.WAITING_FOR_OPPONENT_MOVE);
            userRepository.save(user);
            return List.of(reply);
        }


    }
    private List<PartialBotApiMethod<? extends Serializable>> ending(User winner, User loser) {
        Move winnerMove = moveRepository.getMoveByUserId(winner).orElseThrow();
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
        messageForWinner.setText(String.format("Вы победили!!!\uD83C\uDFC6\uD83C\uDFC6\uD83C\uDFC6\n+%d золотых монет\uD83D\uDCB0", fight.getBet()));
        messageForLoser.setText(String.format("Вы проиграли\uD83D\uDE13\n-%d золотых монет\uD83D\uDCB0", fight.getBet()));

        winner.setUserState(BotState.NONE);
        loser.setUserState(BotState.NONE);

        winner.setGold(winner.getGold() + fight.getBet());
        loser.setGold(loser.getGold() - fight.getBet());

        loser.setCurrentHealth(loser.getCurrentHealth() - (long)(loser.getMaxHealth() * healthCoef));
        userRepository.save(winner);
        userRepository.save(loser);
        moveRepository.delete(winnerMove);
        moveRepository.delete(moveRepository.getMoveByUserId(loser).orElseThrow());
        return List.of(moveMessageForWinner, messageForWinner, moveMessageForLoser, messageForLoser);
    }
    private List<PartialBotApiMethod<? extends Serializable>> useSkill(User user, String message) {
        var messageToUser = createMessageTemplate(user);
        String[] messageMass = message.split("_");
        if(messageMass.length<2){
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
        long mana = user.getCurrentMana() - skill.getManaCost();
        user.setCurrentMana(mana);
        userRepository.save(user);
        return check(user);
    }
    private SendMessage[] createMessage(User actor, User opponent) {
        Move actorMove = moveRepository.getMoveByUserId(actor).orElseThrow();
        Move opponentMove = moveRepository.getMoveByUserId(opponent).orElseThrow();
        var messageForActor = createMessageTemplate(actor);
        var messageForOpponent = createMessageTemplate(opponent);

        String hitMessage = """
                %s
                🛡Соперник защищал: %s

                %s
                🛡Вы защищали: %s

                """;
        String actorHitMessage;
        String actorAttack;
        String actorAttackToOpponent;
        if (actorMove.getActiveSkillId() != null) {
            actorAttack = "☄\uFE0FИспользован навык " + actorMove.getActiveSkillId().getName();
            actorAttackToOpponent = "☄\uFE0FСоперник использовал навык " + actorMove.getActiveSkillId().getName();
        } else {
            actorAttack = "\uD83D\uDDE1Вы били: " + actorMove.getAttack().getTitle();
            actorAttackToOpponent = "\uD83D\uDDE1Соперник бил: " + actorMove.getAttack().getTitle();
        }
        String opponentAttack;
        String opponentAttackToActor;
        if (opponentMove.getActiveSkillId() != null) {
            opponentAttack = "☄\uFE0FИспользован навык " + opponentMove.getActiveSkillId().getName();
            opponentAttackToActor = "☄\uFE0FСоперник использовал навык " + opponentMove.getActiveSkillId().getName();
        } else {
            opponentAttack = "\uD83D\uDDE1Вы били: " + opponentMove.getAttack().getTitle();
            opponentAttackToActor = "\uD83D\uDDE1Соперник бил: " + opponentMove.getAttack().getTitle();
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
        String message = "%s♥️Ваше HP: %s/%s\n ♥️HP Соперника: %s/%s";
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
        messageForActor.setText(String.format(message, actorHitMessage, actorHp, actor.getMaxHealth(), opponentHp, opponent.getMaxHealth()));
        messageForOpponent.setText(String.format(message, opponentHitMessage, opponentHp, opponent.getMaxHealth(), actorHp, actor.getMaxHealth()));

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
