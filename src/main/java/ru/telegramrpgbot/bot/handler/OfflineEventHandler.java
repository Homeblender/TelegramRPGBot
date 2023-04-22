package ru.telegramrpgbot.bot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.enums.EventState;
import ru.telegramrpgbot.bot.enums.EventType;
import ru.telegramrpgbot.bot.util.TelegramUtil;
import ru.telegramrpgbot.model.GroupChat;
import ru.telegramrpgbot.model.OfflineEvent;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.GroupChatRepository;
import ru.telegramrpgbot.repository.OfflineEventRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.telegramrpgbot.bot.util.TelegramUtil.*;

@Component
@Slf4j
public class OfflineEventHandler implements Handler {

    public static final String EVENT_NAME_ACCEPT = "/enter_event_name_accept";
    public static final String EVENT_TYPE_ADMIN = EventType.ADMIN.name();
    public static final String EVENT_TYPE_USER = EventType.USER.name();

    private final OfflineEventRepository offlineEventRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    public OfflineEventHandler(OfflineEventRepository offlineEventRepository, GroupChatRepository groupChatRepository, UserRepository userRepository) {
        this.offlineEventRepository = offlineEventRepository;
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        if (message.equalsIgnoreCase(EVENT_NAME_ACCEPT)) {
            return eventGoalEnter(user);
        } else if (message.toUpperCase().substring(1).equals(Command.CREATE_EVENT.name())) {
            return startCreateEvent(user);
        } else if (message.equals(EVENT_TYPE_ADMIN) || message.equals(EVENT_TYPE_USER)) {
            return savingType(user, message);
        } else if (user.getUserState() == BotState.WAITING_FOR_EVENT_NAME) {
            return savingName(user, message);
        } else if (user.getUserState() == BotState.WAITING_FOR_EVENT_GOAL) {
            return savingGoal(user, message);
        } else if (user.getUserState() == BotState.WAITING_FOR_EVENT_REWARD) {
            return savingReward(user, message);
        }
        return showEvents(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> savingReward(User user, String message) {
        var reply = createMessageTemplate(user);
        var event = offlineEventRepository.findByCreatorAndEventState(user, EventState.CREATING);

        long reward = 0;
        try {
            reward = Long.parseLong(message);
        } catch (NumberFormatException exception) {
            reply.setText("Введите число!");
            return List.of(reply);
        }
        if (reward < 1) {
            reply.setText("Введите положительное число!");
            return List.of(reply);
        }
        if (event.getEventType() == EventType.USER && reward > user.getOfflinePoints()) {
            reply.setText("У вас недостаточно очков оффлайн событий!");
            return List.of(reply);
        }
        if (event.getEventType() == EventType.USER) {
            user.setOfflinePoints(user.getOfflinePoints() - reward);
        }
        reply.setText(String.format("Вы успешно создали событие *%s*! \uD83E\uDD42", event.getEventName()));
        reply.setReplyMarkup(createBaseReplyKeyboard());
        var announcement = String.format("Игрок [%s](tg://user?id=%d) создал событие _%s_", user.getName(), user.getChatId(), event.getEventName());
        var chats = groupChatRepository.findAll();
        List<PartialBotApiMethod<? extends Serializable>> sendMessageList = new ArrayList<>();
        for (GroupChat groupChat : chats) {
            var messageTemplate = createMessageTemplate(groupChat.getId().toString());
            messageTemplate.setText(announcement);
            sendMessageList.add(messageTemplate);
        }

        sendMessageList.add(reply);
        event.setOfflinePointsReward(reward);
        event.setEventState(EventState.ACTIVE);
        user.setUserState(BotState.NONE);
        offlineEventRepository.save(event);
        userRepository.save(user);
        return sendMessageList;
    }

    private List<PartialBotApiMethod<? extends Serializable>> savingType(User user, String message) {
        var reply = createMessageTemplate(user);
        var event = offlineEventRepository.findByCreatorAndEventState(user, EventState.CREATING);
        if (message.equals(EVENT_TYPE_ADMIN) && !user.getIsGameMaster()) {
            reply.setReplyMarkup(createBaseReplyKeyboard());
            reply.setText("Вы не администратор. \uD83D\uDE1F");
            user.setUserState(BotState.NONE);
            userRepository.save(user);
            offlineEventRepository.delete(event);
            return List.of(reply);
        }
        if (message.equals(EVENT_TYPE_USER) && user.getOfflinePoints() < 1) {
            reply.setReplyMarkup(createBaseReplyKeyboard());
            reply.setText("У вас нет очков оффлайн ивентов. \uD83D\uDE1F");
            user.setUserState(BotState.NONE);
            userRepository.save(user);
            offlineEventRepository.delete(event);
            return List.of(reply);
        }
        event.setEventType(Arrays.stream(EventType.values()).filter(eventType -> eventType.name().equals(message)).findFirst().orElseThrow());
        offlineEventRepository.save(event);
        user.setUserState(BotState.WAITING_FOR_EVENT_REWARD);
        userRepository.save(user);
        reply.setText("Введите колличество очков \uD83D\uDC8E в награду за событие.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> savingGoal(User user, String message) {
        var reply = createMessageTemplate(user);
        var event = offlineEventRepository.findByCreatorAndEventState(user, EventState.CREATING);
        event.setEventGoal(message);
        user.setUserState(BotState.WAITING_FOR_EVENT_TYPE);
        userRepository.save(user);
        offlineEventRepository.save(event);
        reply.setText("Выберите тип события.");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton("От пользователя.", EventType.USER.name()),
                createInlineKeyboardButton("От администратора.", EventType.ADMIN.name()));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));
        reply.setReplyMarkup(inlineKeyboardMarkup);
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> eventGoalEnter(User user) {
        var reply = TelegramUtil.createMessageTemplate(user);
        user.setUserState(BotState.WAITING_FOR_EVENT_GOAL);
        userRepository.save(user);
        reply.setText("Введите цель для своего события.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> savingName(User user, String message) {
        var reply = createMessageTemplate(user);
        var event = offlineEventRepository.findByCreatorAndEventState(user, EventState.CREATING);
        event.setEventName(message);
        offlineEventRepository.save(event);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton("Да.", EVENT_NAME_ACCEPT));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));


        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText(String.format("Сохранить название: %s?", event.getEventName()));
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> startCreateEvent(User user) {
        var reply = TelegramUtil.createMessageTemplate(user);
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        user.setUserState(BotState.WAITING_FOR_EVENT_NAME);
        userRepository.save(user);
        offlineEventRepository.save(OfflineEvent.builder().creator(user).build());
        reply.setText("Введите название для своего события.");
        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> showEvents(User user) {
        var reply = TelegramUtil.createMessageTemplate(user);
        if (user.getUserState() != BotState.NONE) {
            reply.setText("Вы сейчас заняты!");
            return List.of(reply);
        }
        StringBuilder replyMessage = new StringBuilder();

        var admins = offlineEventRepository.findAllByEventTypeAndEventState(EventType.ADMIN, EventState.ACTIVE);
        if (admins.isEmpty()) {
            replyMessage.append(String.format("Сейчас нет событий *от администраторов*.%n"));
        } else {
            replyMessage.append(String.format("Активные события *от администраторов*:%n"));
            for (OfflineEvent offlineEvent : admins) {
                replyMessage.append(String.format("%n\uD83C\uDF96 *%s* от [%s](tg://user?id=%d) %n_Цель_ - %s%n_Награда_: %d\uD83D\uDC8E%n", offlineEvent.getEventName(), offlineEvent.getCreator().getName(), offlineEvent.getCreator().getChatId(), offlineEvent.getEventGoal(), offlineEvent.getOfflinePointsReward()));
                if (user.getIsGameMaster()) {
                    replyMessage.append(String.format("Завершить событие - /finish\\_%d\\_[имя победителя]%n", offlineEvent.getId()));
                    replyMessage.append(String.format("Отменить событие - /cancel\\_%d%n", offlineEvent.getId()));
                }
            }
        }
        var users = offlineEventRepository.findAllByEventTypeAndEventState(EventType.USER, EventState.ACTIVE);
        if (users.isEmpty()) {
            replyMessage.append(String.format("%nСейчас нет событий *от пользователей*.%n"));
        } else {
            replyMessage.append(String.format("%nАктивные события *от пользователей*:%n"));
            for (OfflineEvent offlineEvent : users) {
                replyMessage.append(String.format("%n\uD83C\uDF96 *%s* от [%s](tg://user?id=%d)%n_Цель_ - %s%n_Награда_: %d\uD83D\uDC8E%n", offlineEvent.getEventName(), offlineEvent.getCreator().getName(), offlineEvent.getCreator().getChatId(), offlineEvent.getEventGoal(), offlineEvent.getOfflinePointsReward()));
                if (offlineEvent.getCreator().equals(user)) {
                    replyMessage.append(String.format("Завершить событие - /finish\\_%d\\_[имя победителя]%n", offlineEvent.getId()));
                    replyMessage.append(String.format("Отменить событие - /cancel\\_%d%n", offlineEvent.getId()));
                }
            }
        }
        replyMessage.append("\nВы можете создать событие командой - /create\\_event.");
        if (user.getIsGameMaster()) {
            replyMessage.append("\nВы *администратор*. \uD83D\uDE0E");
        }
        reply.setText(replyMessage.toString());
        return List.of(reply);
    }

    @Override
    public List<BotState> operatedBotState() {
        return List.of(BotState.WAITING_FOR_EVENT_NAME, BotState.WAITING_FOR_EVENT_TYPE, BotState.WAITING_FOR_EVENT_GOAL, BotState.WAITING_FOR_EVENT_REWARD);
    }

    @Override
    public List<Command> operatedCommand() {
        return List.of(Command.CREATE_EVENT, Command.SHOW_EVENTS);
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(EVENT_NAME_ACCEPT, EVENT_TYPE_ADMIN, EVENT_TYPE_USER);
    }
}
