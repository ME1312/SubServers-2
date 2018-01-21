package net.ME1312.SubServers.Host.Library;

import org.fusesource.jansi.Ansi;

import java.util.Arrays;

/**
 * Color Code Converter Enum
 */
public enum TextColor {
    AQUA('b', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold()),
    BLACK('0', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff()),
    BLUE('9', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold()),
    BOLD('l', Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE)),
    DARK_AQUA('3', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff()),
    DARK_BLUE('1', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff()),
    DARK_GRAY('8', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold()),
    DARK_GREEN('2', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff()),
    DARK_PURPLE('5', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff()),
    DARK_RED('4', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff()),
    GOLD('6', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff()),
    GRAY('7', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff()),
    GREEN('a', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold()),
    ITALIC('o', Ansi.ansi().a(Ansi.Attribute.ITALIC)),
    LIGHT_PURPLE('d', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold()),
    MAGIC('k', Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW)),
    RED('c', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold()),
    RESET('r', Ansi.ansi().a(Ansi.Attribute.RESET)),
    STRIKETHROUGH('m', Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON)),
    UNDERLINE('n', Ansi.ansi().a(Ansi.Attribute.UNDERLINE)),
    WHITE('f', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold()),
    YELLOW('e', Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold());

    private final Character minecraft;
    private final Ansi console;

    TextColor(Character minecraft, Ansi console) {
        this.minecraft = minecraft;
        this.console = console;
    }

    /**
     * Get this color as a Minecraft Color Code
     *
     * @return Minecraft Color Code
     */
    public String asMinecraftCode() {
        return new String(new char[]{'\u00A7', minecraft});
    }

    /**
     * Get this color as an Ansi Color Code
     *
     * @return Ansi Color Code
     */
    public String asAnsiCode() {
        return console.toString();
    }

    @Override
    public String toString() {
        return asAnsiCode();
    }

    /**
     * Parse Minecraft color codes starting with character
     *
     * @param character Character
     * @param str String to parse
     * @return Minecraft colored string
     */
    public static String parseColor(char character, String str) {
        for (TextColor color : Arrays.asList(TextColor.values())) {
            str = str.replace(new String(new char[]{character, color.minecraft}), color.asMinecraftCode());
        }
        return str;
    }

    /**
     * Convert Minecraft color codes to Ansi color codes
     *
     * @param str Minecraft colored string
     * @return Ansi colored string
     */
    public static String convertColor(String str) {
        return convertColor('\u00A7', str);
    }

    /**
     * Convert Minecraft color codes starting with character to Ansi color codes
     *
     * @param character Character
     * @param str String to parse
     * @return Ansi colored string
     */
    public static String convertColor(char character, String str) {
        for (TextColor color : Arrays.asList(TextColor.values())) {
            str = str.replace(new String(new char[]{character, color.minecraft}), color.asAnsiCode());
        }
        return str;
    }

    /**
     * Removes all Minecraft/Ansi color codes from a string
     *
     * @param str String to parse
     * @return String without color
     */
    public static String stripColor(String str) {
        for (TextColor color : Arrays.asList(TextColor.values())) {
            str = str.replace(color.asMinecraftCode(), "").replace(color.asAnsiCode(), "");
        }
        return str;
    }
}
