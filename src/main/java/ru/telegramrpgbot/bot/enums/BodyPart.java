package ru.telegramrpgbot.bot.enums;
public enum BodyPart {

    HEAD ("голова"),
    CHEST ("грудь"),
    LEGS ("ноги");

    private final String title;

    BodyPart(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }
}
