package ru.telegramrpgbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TelegramRpgBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelegramRpgBotApplication.class, args);
    }

}
