package ru.telegramrpgbot.bot.enums;

public enum Command {
    HERO ("\uD83D\uDC64"),
    ADVENTURES ("\uD83C\uDFC3\uD83C\uDFFC\u200D♂️"),
    CITY("\uD83C\uDF06"),
    FORGE("⚒"),
    SHOP("\uD83D\uDED2"),
    BACK("⬅️"),
    WEAPON_AND_SHIELDS("\uD83D\uDDE1"),
    ARMOR("\uD83C\uDFBD"),
    SELL(null),
    INVENTORY(null),
    EQUIP(null),
    UNEQUIP(null),
    SHARP(null),
    BUY(null),
    FIGHT(null),
    CLASSES(null),
    EQUIPMENT(null),
    ACCEPT("Принять"),
    CANCEL("Отменить"),
    CLASS(null);
    private final String russian;
    public String getRussian() {

        return russian == null?"":russian;
    }
    Command(String russian) {
        this.russian = russian;
    }
}
