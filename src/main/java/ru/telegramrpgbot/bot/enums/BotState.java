package ru.telegramrpgbot.bot.enums;

public enum BotState {
    START(null),
    WAITING_FOR_NAME(null),
    NONE("Отдых \uD83D\uDCA4"),
    WAITING_FOR_MOVE("Сражение"),
    WAITING_FOR_ANSWER("Ответ на предложение"),
    WAITING_FOR_EVENT_NAME("Создание события"),
    WAITING_FOR_EVENT_GOAL("Создание события"),
    WAITING_FOR_EVENT_TYPE("Создание события"),
    WAITING_FOR_EVENT_REWARD("Создание события"),
    WAITING_FOR_ANSWER_TO_INVITE("Ответ на приглашение"),
    WAITING_FOR_OPPONENT("Сражение"),
    WAITING_FOR_OPPONENT_MOVE("Сражение"),
    WAITING_FOR_PARTY_NAME("Создание группы"),
    RAIDING("Рейд на босса"),
    SOLO_ACTIVITY("ACTIVITY");
    private final String title;
    public String getTitle() {
        return title == null?"":title;
    }
    BotState(String title) {
        this.title = title;
    }
}
