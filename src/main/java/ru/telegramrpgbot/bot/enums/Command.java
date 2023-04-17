package ru.telegramrpgbot.bot.enums;

public enum Command {
    HERO ("Персонаж"),
    ADVENTURES ("Исследования"),
    CHANGE_NAME(null),
    CITY("Город"),
    FORGE("Кузница"),
    SHOP("Магазин"),
    BACK("Назад"),
    SELL(null),
    INVENTORY(null),
    EQUIP(null),
    UNEQUIP(null),
    EQUIPMENT(null);
    private final String russian;
    public String getRussian() {

        return russian == null?"":russian;
    }
    Command(String russian) {
        this.russian = russian;
    }
}
