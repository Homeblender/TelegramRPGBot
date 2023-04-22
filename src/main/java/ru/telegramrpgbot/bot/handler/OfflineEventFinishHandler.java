package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.EventState;
import ru.telegramrpgbot.bot.enums.EventType;
import ru.telegramrpgbot.model.OfflineEvent;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.GroupChatRepository;
import ru.telegramrpgbot.repository.OfflineEventRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.List;

import static ru.telegramrpgbot.bot.util.IngameUtil.Announcement;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createBaseReplyKeyboard;
import static ru.telegramrpgbot.bot.util.TelegramUtil.createMessageTemplate;

@Component
@Slf4j
public class OfflineEventFinishHandler implements Handler {

    private final OfflineEventRepository offlineEventRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    public OfflineEventFinishHandler(OfflineEventRepository offlineEventRepository, GroupChatRepository groupChatRepository, UserRepository userRepository) {
        this.offlineEventRepository = offlineEventRepository;
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }


    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.substring(1).split("_")[0].toUpperCase().equals(Command.ANNUL.name())){
            return cancelEvent(user, message);
        }else if (message.substring(1).split("_")[0].toUpperCase().equals(Command.FINISH.name())){
            return finishEvent(user, message);
        }
        return null;
    }

    private List<PartialBotApiMethod<? extends Serializable>> finishEvent(User user, String message) {
        var replyToUser = createMessageTemplate(user);
        String[] messageMass = message.split("_");
        OfflineEvent event;
        User winner;
        if (messageMass.length < 2) {
            replyToUser.setText("Не указано id события.");
            return List.of(replyToUser);
        }
        if (messageMass.length < 3) {
            replyToUser.setText("Не указано имя победителя.");
            return List.of(replyToUser);
        }
        try {
            event = offlineEventRepository.findById(Long.parseLong(messageMass[1]));
        } catch (NumberFormatException exception) {
            replyToUser.setText("Нет такого события.");
            return List.of(replyToUser);
        }
        try {
            winner = userRepository.findAllByName(messageMass[2]).orElseThrow();
        } catch (Exception exception) {
            replyToUser.setText("Нет такого игрока.");
            return List.of(replyToUser);
        }
        if (winner.equals(user) || event.getCreator().equals(winner)){
            replyToUser.setText("Нельзя выбрать себя либо создателя ивента.");
            return List.of(replyToUser);
        }
        if ((event.getEventType() == EventType.ADMIN && !user.getIsGameMaster())||(event.getEventType() == EventType.USER && !event.getCreator().equals(user))) {
            replyToUser.setText("Вы не можете завершить это событие.");
            return List.of(replyToUser);
        }
        var messageToWinner = createMessageTemplate(winner);
        replyToUser.setText(String.format("Вы успешно завершили событие _%s_ \uD83C\uDF7E",event.getEventName()));
        replyToUser.setReplyMarkup(createBaseReplyKeyboard());
        messageToWinner.setText(String.format("\uD83C\uDF7E Вы победили в событии _%s_ и получили %d \uD83D\uDC8E. ",event.getEventName(),event.getOfflinePointsReward()));
        event.setEventState(EventState.FINISHED);
        winner.setOfflinePoints(winner.getOfflinePoints()+event.getOfflinePointsReward());
        userRepository.save(winner);
        offlineEventRepository.save(event);

        var announcement = String.format("Игрок [%s](tg://user?id=%d) подебил в событии _%s_ \uD83C\uDF7E", winner.getName(), winner.getChatId(), event.getEventName());
        var sendMessageList = Announcement(announcement);

        sendMessageList.add(replyToUser);
        sendMessageList.add(messageToWinner);
        return sendMessageList;
    }

    private List<PartialBotApiMethod<? extends Serializable>> cancelEvent(User user, String message) {
        var reply = createMessageTemplate(user);

        String[] messageMass = message.split("_");
        OfflineEvent event;
        if (messageMass.length < 2) {
            reply.setText("Не указано id события.");
            return List.of(reply);
        }
        if (messageMass.length > 2) {
            reply.setText("Можно указать имя только одного события.");
            return List.of(reply);
        }
        try {
            event = offlineEventRepository.findById(Long.parseLong(messageMass[1]));
        } catch (NumberFormatException exception) {
            reply.setText("Нет такого события.");
            return List.of(reply);
        }
        if ((event.getEventType() == EventType.ADMIN && !user.getIsGameMaster())||(event.getEventType() == EventType.USER && !event.getCreator().equals(user))) {
            reply.setText("Вы не можете отменить это событие.");
            return List.of(reply);
        }
        if (event.getEventType() == EventType.USER){
            user.setOfflinePoints(user.getOfflinePoints()+event.getOfflinePointsReward());
            userRepository.save(user);
        }
        var announcement = String.format("Игрок [%s](tg://user?id=%d) отменил событие _%s_ \uD83D\uDE1F", user.getName(), user.getChatId(), event.getEventName());
        reply.setText(String.format("Вы отменили событие _%s_",event.getEventName()));
        var sendMessageList = Announcement(announcement);
        event.setEventState(EventState.CANCELED);
        offlineEventRepository.save(event);
        sendMessageList.add(reply);
        return sendMessageList;
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of();
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.ANNUL,Command.FINISH);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of();
    }
}
