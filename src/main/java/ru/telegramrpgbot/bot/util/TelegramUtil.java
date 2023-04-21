package ru.telegramrpgbot.bot.util;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.telegramrpgbot.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TelegramUtil {
    public static SendMessage createMessageTemplate(User user) {
        return createMessageTemplate(String.valueOf(user.getChatId()));
    }
    public static SendMessage createMessageTemplate(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    public static InlineKeyboardButton createInlineKeyboardButton(String text, String command) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(text);
        inlineKeyboardButton.setCallbackData(command);
        return inlineKeyboardButton;
    }

    public static ReplyKeyboard createBaseReplyKeyboard(){

        var buttons = new KeyboardButton[] {
                new KeyboardButton("\uD83D\uDC64 Персонаж"),
                new KeyboardButton("\uD83C\uDFC3\uD83C\uDFFC\u200D\u2642\uFE0F Исследования"),
                new KeyboardButton("\uD83C\uDF06 Город"),
                new KeyboardButton("\uD83C\uDF89 Пати")
        };

        return createKeyboard(buttons);
    }

    public static ReplyKeyboardMarkup createKeyboard(KeyboardButton[] buttons) {
        KeyboardRow row = new KeyboardRow(2);
        row.addAll(Arrays.asList(buttons));
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row);

        var newReplyKeyboard = new ReplyKeyboardMarkup(rows);
        newReplyKeyboard.setResizeKeyboard(true);
        return newReplyKeyboard;
    }

    public static List<PartialBotApiMethod<? extends Serializable>> busyReply(User user) {
        var reply = createMessageTemplate(user);
        reply.setText("Сейчас ты занят другим.");
        return List.of(reply);
    }
}
