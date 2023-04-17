package ru.telegramrpgbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.telegramrpgbot.bot.enums.BotState;
import ru.telegramrpgbot.bot.enums.Command;
import ru.telegramrpgbot.bot.handler.Handler;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.ClassRepository;
import ru.telegramrpgbot.repository.UserRepository;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

@Component
@Slf4j
public class UpdateReceiver {
    private final List<Handler> handlers;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;

    public UpdateReceiver(List<Handler> handlers, UserRepository userRepository, ClassRepository classRepository) {
        this.handlers = handlers;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
    }

    public List<PartialBotApiMethod<? extends Serializable>> handle(Update update) {
        if (isMessageWithText(update)) {
            Message message = update.getMessage();
            String messageText = update.getMessage().getText().toUpperCase();

            Long chatId = message.getFrom().getId();
            User user = userRepository.getUserByChatId(chatId).orElseGet(() ->
                    userRepository.save(User.builder()
                            .chatId(chatId)
                            .name(update.getMessage().getChat().getFirstName())
                                    .userClass(classRepository.findById(1L).orElseThrow())
                            .build()));


            Handler handler = getHandlerByState(user.getUserState());
            if (handler == null) {
                try {
                    String t = messageText.split(" ")[1];

                    handler = getHandlerByCommand(Arrays.stream(Command.values()).filter(command -> command.getRussian().toUpperCase().equals(t)).findFirst().get());
                } catch (Exception exception) { log.info(exception.getMessage());}
                try {
                    handler = getHandlerByCommand(Command.valueOf(messageText.substring(1)));
                } catch (Exception exception) {log.info(exception.getMessage());}
                try {
                    handler = getHandlerByCommand(Command.valueOf(messageText.substring(1).split("_")[0]));
                } catch (Exception exception) {log.info(exception.getMessage());}

            }
            return handler==null?List.of(): handler.handle(user, message.getText());

        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getFrom().getId();
            User user = userRepository.getUserByChatId(chatId)
                    .orElseGet(() -> userRepository.save(User.builder().chatId(chatId).name(update.getMessage().getChat().getFirstName()).build()));

            return getHandlerByCallBackQuery(callbackQuery.getData()).handle(user, callbackQuery.getData());
        }
        return Collections.emptyList();
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
                        .anyMatch(query::startsWith))
                .findAny()
                .orElse(null);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }

}
