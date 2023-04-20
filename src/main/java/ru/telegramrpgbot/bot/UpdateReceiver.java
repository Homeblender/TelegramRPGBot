package ru.telegramrpgbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.handler.Handler;
import ru.telegramrpgbot.bot.handler.UserDataHandler;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.ClassRepository;
import ru.telegramrpgbot.repository.UserRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class UpdateReceiver {
    private final List<Handler> handlers;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final UserDataHandler userDataHandler;

    public UpdateReceiver(List<Handler> handlers, UserRepository userRepository, ClassRepository classRepository, UserDataHandler userDataHandler) {
        this.handlers = handlers;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.userDataHandler = userDataHandler;
    }

    public List<PartialBotApiMethod<? extends Serializable>> handle(Update update) {
        if (isMessageWithText(update)) {
            Message message = update.getMessage();
            String messageText = update.getMessage().getText().toUpperCase();

            Long chatId = message.getFrom().getId();
            User user = userRepository.getUserByChatId(chatId).orElseGet(() ->
                    userRepository.save(User.builder()
                            .chatId(chatId)
                            .name(update.getMessage().getChat().getFirstName() + "TEMP0")
                            .userClass(classRepository.findById(1L).orElseThrow())
                            .build()));


            Handler handler = getHandlerByState(user.getUserState());
            if (handler == null) {
                try {
                    String t = messageText.split(" ")[0];
                    handler = getHandlerByCommand(Arrays.stream(Command.values()).filter(command -> command.getRussian().toUpperCase().equals(t)).findFirst().orElseThrow());
                } catch (Exception ignored) {
                }
                try {
                    handler = getHandlerByCommand(Command.valueOf(messageText.substring(1)));
                } catch (Exception ignored) {
                }
                try {
                    handler = getHandlerByCommand(Command.valueOf(messageText.substring(1).split("_")[0]));
                } catch (Exception ignored) {
                }

            }
            return handler == null ? userDataHandler.handle(user,message.getText()) : handler.handle(user, message.getText());

        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getFrom().getId();
            User user = userRepository.getUserByChatId(chatId)
                    .orElseGet(() -> userRepository.save(User.builder().chatId(chatId).name(update.getMessage().getChat().getFirstName() + "TEMP0").build()));
            Handler handler = getHandlerByCallBackQuery(callbackQuery.getData());
            if (handler == null) {
                try {
                    handler = getHandlerByCallBackQuery(callbackQuery.getData().toUpperCase().split("_")[1]);
                } catch (Exception ignored) {
                }
                try {
                    handler = getHandlerByCallBackQuery(callbackQuery.getData().toUpperCase().split("_")[0]);
                } catch (Exception ignored) {
                }
            }
            List<PartialBotApiMethod<? extends Serializable>> allMessages = handler.handle(user, callbackQuery.getData()).stream().toList();
//            log.info(allMessages.size()+"");
//            log.info(((SendMessage)allMessages.get(0)).getChatId()+"  " + user.getChatId().toString());
//            log.info(((SendMessage)allMessages.get(1)).getChatId()+"  " + user.getChatId().toString());
            SendMessage messageToKeyboard =(SendMessage) allMessages.stream().filter(w-> w instanceof SendMessage && ((SendMessage) w).getChatId().equals(user.getChatId().toString())).findFirst().orElseThrow();

            var otherMessages = new ArrayList<>(allMessages.stream().filter(w -> w instanceof SendMessage && !((SendMessage) w).getChatId().equals(user.getChatId().toString())).toList());
            EditMessageText new_message = new EditMessageText();
            new_message.setChatId(messageToKeyboard.getChatId());
            new_message.setMessageId(callbackQuery.getMessage().getMessageId());
            new_message.setText(messageToKeyboard.getText());
            new_message.enableMarkdown(true);
            try {
                new_message.setReplyMarkup((InlineKeyboardMarkup) messageToKeyboard.getReplyMarkup());
            }catch (Exception ignored){ }
            if (!otherMessages.isEmpty()){
                otherMessages.add(new_message);
                return otherMessages;

            }
            return List.of(new_message);
        }
        return List.of();
    }

    private Handler getHandlerByState(BotState state) {
        return handlers.stream()
                .filter(h -> h.operatedBotState() != null)
                .filter(h -> h.operatedBotState().stream()
                        .anyMatch(state::equals))
                .findAny()
                .orElse(null);
    }

    private Handler getHandlerByCommand(Command command) {
        return handlers.stream()
                .filter(h -> h.operatedCommand() != null)
                .filter(h -> h.operatedCommand().stream()
                        .anyMatch(command::equals))
                .findAny()
                .orElse(null);
    }

    private Handler getHandlerByCallBackQuery(String query) {
        return handlers.stream()
                .filter(h -> h.operatedCallBackQuery().stream()
                        .anyMatch(query::equals))
                .findAny()
                .orElse(null);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }

}
