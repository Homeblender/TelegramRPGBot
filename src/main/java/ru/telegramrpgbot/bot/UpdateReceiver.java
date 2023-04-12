package ru.telegramrpgbot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.telegramrpgbot.bot.handler.Handler;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.UserRepository;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.io.Serializable;
import java.util.List;
import java.util.Collections;

@Component
@Slf4j
public class UpdateReceiver {
    private final List<Handler> handlers;
    private final UserRepository userRepository;

    public UpdateReceiver(List<Handler> handlers, UserRepository userRepository) {
        this.handlers = handlers;
        this.userRepository = userRepository;
    }

    public List<PartialBotApiMethod<? extends Serializable>> handle(Update update) {
            if (isMessageWithText(update)) {
                Message message = update.getMessage();
                Long chatId = message.getFrom().getId();
                User user = userRepository.getUserByChatId(chatId)
                        .orElseGet(() -> userRepository.save(User.builder().chatId(chatId).name(update.getMessage().getChat().getFirstName()).build()));
                return getHandlerByState(user.getCurrentUserState()).handle(user, message.getText());

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
                .filter(h -> h.operatedBotState().equals(state))
                .findAny()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private Handler getHandlerByCallBackQuery(String query) {
        return handlers.stream()
                .filter(h -> h.operatedCallBackQuery().stream()
                        .anyMatch(query::startsWith))
                .findAny()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }

}
