package ru.telegramrpgbot.bot.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.model.Fight;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.InventoryCellRepository;
import ru.telegramrpgbot.repository.UserRepository;
import ru.telegramrpgbot.repository.FightRepository;

import java.io.Serializable;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class FightHandler implements Handler {
    private final UserRepository userRepository;
    private final FightRepository fightRepository;

    public FightHandler(UserRepository userRepository, FightRepository fightRepository) {
        this.userRepository = userRepository;
        this.fightRepository = fightRepository;
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
