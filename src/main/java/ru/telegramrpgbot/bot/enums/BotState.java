package ru.telegramrpgbot.bot.enums;

public enum BotState {
    START(null),
    WAITING_FOR_NAME(null),
    NONE("Отдых \uD83D\uDCA4"),
    WAITING_FOR_MOVE(null),
    WAITING_FOR_ANSWER(null),
    WAITING_FOR_OPPONENT(null),
    WAITING_FOR_OPPONENT_MOVE(null),
    SOLO_ACTIVITY("ACTIVITY");
    private final String title;
    public String getTitle() {

        return title == null?"":title;
    }
    BotState(String russian) {
        this.title = russian;
    }
}
