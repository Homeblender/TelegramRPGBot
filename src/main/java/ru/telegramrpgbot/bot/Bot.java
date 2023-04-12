package ru.telegramrpgbot.bot;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private final UpdateReceiver updateReceiver;
    public Bot(UpdateReceiver updateReceiver){
        this.updateReceiver = updateReceiver;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        var messagesToSend = updateReceiver.handle(update);

        if (messagesToSend != null && !messagesToSend.isEmpty()) {
            messagesToSend.forEach(response -> {
                if (response instanceof SendMessage) {
                    executeWithExceptionCheck((SendMessage) response);
                }
            });
        }
    }

    private void executeWithExceptionCheck(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("govno");
        }
    }

}
