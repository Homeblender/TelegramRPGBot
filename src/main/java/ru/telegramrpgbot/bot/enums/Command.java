package ru.telegramrpgbot.bot.enums;

public enum Command {
    HERO ("\uD83D\uDC64"),
    CHANGE_NAME ("\uD83D\uDC64"),
    ADVENTURES ("\uD83C\uDFC3\uD83C\uDFFC\u200D♂️"),
    CITY("\uD83C\uDF06"),
    PARTY("\uD83C\uDF89"),
    BOSSES("\uD83D\uDC7E"),
    CREATE_PARTY("Создать группу"),
    INVITE(null),
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
    GRANT(null),
    USE(null),
    PASSIVES(null),
    APPLY(null),
    RELEARN(null),
    GUIDE(null),
    CRAFT(null),
    RAID(null),
    ACCEPT("Принять"),
    CANCEL("Отменить"),
    PROPOSE(null),
    ACCEPT_PROPOSE("Принять"),
    CANCEL_PROPOSE("Отменить"),
    ACCEPT_INVITE("Принять"),
    CANCEL_INVITE("Отменить"),
    USE_ACTIVE_SKILL("Скиллы"),
    SKILL(null),
    EXIT(null),
    DIVORCE(null),
    CREATE_EVENT(null),
    SHOW_EVENTS("\uD83C\uDF96"),
    FINISH(null),
    ANNUL(null),
    TRADE(null),
    CLASS(null);
    private final String russian;
    public String getRussian() {

        return russian == null?"":russian;
    }
    Command(String russian) {
        this.russian = russian;
    }
}
