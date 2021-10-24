package net.ME1312.SubServers.Velocity.Library.Compatibility;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;

/**
 * Color Code Converter Enum
 */
public enum ChatColor {
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

    private final Character minecraft;
    ChatColor(Character minecraft) {
        this.minecraft = minecraft;
    }

    /**
     * Get this color as a Minecraft Color Code
     *
     * @return Minecraft Color Code
     */
    public String asMinecraftCode() {
        return new String(new char[]{'\u00A7', minecraft});
    }

    @Override
    public String toString() {
        return asMinecraftCode();
    }

    /**
     * Parse Minecraft color codes starting with character
     *
     * @param character Character
     * @param str String to parse
     * @return Minecraft colored string
     */
    public static String parseColor(char character, String str) {
        for (ChatColor color : Arrays.asList(ChatColor.values())) {
            str = str.replace(new String(new char[]{character, color.minecraft}), color.asMinecraftCode());
        }
        return str;
    }

    /**
     * Convert Minecraft color codes to Sponge Text
     *
     * @param str Minecraft colored string
     * @return Sponge Text
     */
    public static TextComponent convertColor(String str) {
        return LegacyComponentSerializer.legacySection().deserialize(str);
    }

    /**
     * Convert Minecraft color codes starting with character to Sponge Text
     *
     * @param character Character
     * @param str String to parse
     * @return Sponge Text
     */
    public static TextComponent convertColor(char character, String str) {
        return LegacyComponentSerializer.legacy(character).deserialize(str);
    }

    /**
     * Removes all Minecraft color codes from a string
     *
     * @param str String to parse
     * @return String without color
     */
    public static String stripColor(String str) {
        for (ChatColor color : Arrays.asList(ChatColor.values())) {
            str = str.replace(color.asMinecraftCode(), "");
        }
        return str;
    }
}
