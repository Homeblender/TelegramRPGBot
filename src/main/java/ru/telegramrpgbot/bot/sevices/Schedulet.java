package ru.telegramrpgbot.bot.sevices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class Schedulet {

    @Scheduled(fixedDelay = 2000)
    public void govno() throws InterruptedException {
        log.info("Gonvo Started");
        Thread.sleep(20000);
        log.info("Govno Finished!!!!");
    }

}
