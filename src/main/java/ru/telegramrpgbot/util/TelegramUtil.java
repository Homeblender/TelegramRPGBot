package ru.telegramrpgbot.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.telegramrpgbot.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TelegramUtil {
    public static SendMessage createMessageTemplate(User user) {
        return createMessageTemplate(String.valueOf(user.getChatId()));
    }

    // Создаем шаблон SendMessage с включенным Markdown
    public static SendMessage createMessageTemplate(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    // Создаем кнопку
    public static InlineKeyboardButton createInlineKeyboardButton(String text, String command) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(text);
        inlineKeyboardButton.setCallbackData(command);
        return inlineKeyboardButton;
    }

    public static ReplyKeyboard createBaseReplyKeyboard(){

        var buttons = new KeyboardButton[] { new KeyboardButton("🆔ME"), new KeyboardButton("❔Activity")};
        KeyboardRow row = new KeyboardRow(2);
        row.add(0,buttons[0]);
        row.add(1,buttons[1]);
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row);

        var newReplyKeyboard = new ReplyKeyboardMarkup(rows);
        newReplyKeyboard.setResizeKeyboard(true);
        return newReplyKeyboard;
    }
}
