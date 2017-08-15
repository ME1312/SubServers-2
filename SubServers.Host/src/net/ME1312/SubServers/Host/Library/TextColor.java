package net.ME1312.SubServers.Host.Library;

import java.util.Arrays;

public enum TextColor {
    AQUA('b'),
    BLACK('0'),
    BLUE('9'),
    BOLD('l'),
    DARK_AQUA('3'),
    DARK_BLUE('1'),
    DARK_GRAY('8'),
    DARK_GREEN('2'),
    DARK_PURPLE('5'),
    DARK_RED('4'),
    GOLD('6'),
    GRAY('7'),
    GREEN('a'),
    ITALIC('o'),
    LIGHT_PURPLE('d'),
    MAGIC('k'),
    RED('c'),
    RESET('r'),
    STRIKETHROUGH('m'),
    UNDERLINE('n'),
    WHITE('f'),
    YELLOW('e');

    private final Character value;

    TextColor(Character value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    public String getValue() {
        return "\u00A7" + value;
    }

    public static char getColorChar() {
        return '\u00A7';
    }

    public static String parseColor(char character, String str) {
        str = str.replace(character, '\u00A7');
        return str;
    }

    public static String stripColor(String str) {
        for (TextColor color : Arrays.asList(TextColor.values())) {
            str = str.replace(color.getValue(), "");
        }
        return str;
    }
}
