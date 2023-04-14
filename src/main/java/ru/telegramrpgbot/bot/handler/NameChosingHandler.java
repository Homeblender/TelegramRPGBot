package ru.telegramrpgbot.bot.handler;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.telegramrpgbot.enums.BotState;
import ru.telegramrpgbot.enums.Command;
import ru.telegramrpgbot.model.User;
import ru.telegramrpgbot.repository.UserRepository;
import static ru.telegramrpgbot.util.TelegramUtil.createMessageTemplate;
import static ru.telegramrpgbot.util.TelegramUtil.createInlineKeyboardButton;

import java.io.Serializable;
import java.util.List;
@Component
public class NameChosingHandler implements Handler {
    //Храним поддерживаемые CallBackQuery в виде констант
    public static final String NAME_ACCEPT = "/enter_name_accept";
    public static final String NAME_CHANGE = "/WAITING_FOR_NAME";
    public static final String NAME_CHANGE_CANCEL = "/enter_name_cancel";

    private final UserRepository userRepository;

    public NameChosingHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        // Проверяем тип полученного события
        if (message.equalsIgnoreCase(NAME_ACCEPT) || message.equalsIgnoreCase(NAME_CHANGE_CANCEL)) {
            return accept(user);
        } else if (message.equalsIgnoreCase(NAME_CHANGE)) {
            return changeName(user);
        }
        return checkName(user, message);

    }

    private List<PartialBotApiMethod<? extends Serializable>> accept(User user) {
        // Если пользователь принял имя - меняем статус и сохраняем
        user.setCurrentUserState(BotState.NONE);
        userRepository.save(user);
        var reply = createMessageTemplate(user);
        reply.setText(String.format("Теперь тебя зовут: %s", user.getName()));

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> checkName(User user, String message) {
        user.setName(message);
        var reply = createMessageTemplate(user);

        if (userRepository.getUsersByName(message).orElse(null) != null){
            reply.setText("Это имя уже занято, выбери другое.");
            return null;
        }
        // При проверке имени мы превентивно сохраняем пользователю новое имя в базе
        userRepository.save(user);

        // Делаем кнопку для применения изменений
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton("Да.", NAME_ACCEPT));
        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));


        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText(String.format("Тебя зовут: %s?%nЕсли нет, напиши другое имя.", user.getName()));

        return List.of(reply);
    }

    private List<PartialBotApiMethod<? extends Serializable>> changeName(User user) {
        // При запросе изменения имени мы меняем BotState
        user.setCurrentUserState(BotState.WAITING_FOR_NAME);
        userRepository.save(user);

        // Создаем кнопку для отмены операции
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> inlineKeyboardButtonsRowOne = List.of(
                createInlineKeyboardButton("Cancel", NAME_CHANGE_CANCEL));

        inlineKeyboardMarkup.setKeyboard(List.of(inlineKeyboardButtonsRowOne));

        var reply = createMessageTemplate(user);
        reply.setReplyMarkup(inlineKeyboardMarkup);
        reply.setText(String.format("Тебя зовут: %s?%nЕсли нет, напиши другое имя.", user.getName()));

        return List.of(reply);
    }

    @Override
    public BotState operatedBotState() {
        return BotState.WAITING_FOR_NAME;
    }

    @Override
    public Command operatedCommand() {
        return null;
    }

    @Override
    public List<String> operatedCallBackQuery() {
        return List.of(NAME_ACCEPT, NAME_CHANGE, NAME_CHANGE_CANCEL);
    }
}
