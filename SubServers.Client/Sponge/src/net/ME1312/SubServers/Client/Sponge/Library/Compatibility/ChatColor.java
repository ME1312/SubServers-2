package net.ME1312.SubServers.Client.Sponge.Library.Compatibility;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Color Code Converter Enum
 */
public enum ChatColor {
    AQUA('b', TextColors.AQUA),
    BLACK('0', TextColors.BLACK),
    BLUE('9', TextColors.BLUE),
    BOLD('l', TextStyles.BOLD),
    DARK_AQUA('3', TextColors.DARK_AQUA),
    DARK_BLUE('1', TextColors.DARK_BLUE),
    DARK_GRAY('8', TextColors.DARK_GRAY),
    DARK_GREEN('2', TextColors.DARK_GREEN),
    DARK_PURPLE('5', TextColors.DARK_PURPLE),
    DARK_RED('4', TextColors.DARK_RED),
    GOLD('6', TextColors.GOLD),
    GRAY('7', TextColors.GRAY),
    GREEN('a', TextColors.GREEN),
    ITALIC('o', TextStyles.ITALIC),
    LIGHT_PURPLE('d', TextColors.LIGHT_PURPLE),
    MAGIC('k', TextStyles.OBFUSCATED),
    RED('c', TextColors.RED),
    RESET('r', TextColors.RESET),
    STRIKETHROUGH('m', TextStyles.STRIKETHROUGH),
    UNDERLINE('n', TextStyles.UNDERLINE),
    WHITE('f', TextColors.WHITE),
    YELLOW('e', TextColors.YELLOW);

    private static HashMap<Character, ChatColor> map = new HashMap<Character, ChatColor>();
    private static boolean defaults = false;
    private final Character minecraft;
    private final TextColor color;
    private final TextStyle[] style;

    ChatColor(Character minecraft, TextColor color) {
        this(minecraft, color, TextStyles.RESET);
    }
    ChatColor(Character minecraft, TextStyle... style) {
        this(minecraft, null, style);
    }
    ChatColor(Character minecraft, TextColor color, TextStyle... style) {
        this.minecraft = minecraft;
        this.color = color;
        this.style = style;
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
     * Get the Sponge API equivalent of this value
     *
     * @return Sponge API Color (may be null for modifiers)
     */
    public TextColor asTextColor() {
        return color;
    }

    /**
     * Get the styles this color applies
     *
     * @return Sponge API Styles
     */
    public TextStyle[] getStyles() {
        return style;
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
    public static Text convertColor(String str) {
        return convertColor('\u00A7', str);
    }

    /**
     * Convert Minecraft color codes starting with character to Sponge Text
     *
     * @param character Character
     * @param str String to parse
     * @return Sponge Text
     */
    public static Text convertColor(char character, String str) {
        if (!defaults) {
            for (ChatColor color : ChatColor.values()) map.put(color.minecraft, color);
            defaults = true;
        }

        if (str.contains(Character.toString(character))) {
            LinkedList<String> pieces = new LinkedList<String>(Arrays.asList(str.split(Pattern.quote(Character.toString(character)))));
            Collections.reverse(pieces);

            Text result = null;
            int i = pieces.size();
            for (String piece : pieces) {
                i--;
                Text.Builder current;
                if (i > 0 && piece.length() > 0) {
                    if (map.keySet().contains(piece.toCharArray()[0])) {
                        current = Text.builder(piece.substring(1));
                        ChatColor color = map.get(piece.toCharArray()[0]);
                        current.style(color.getStyles());
                        if (color.asTextColor() != null) current.color(color.asTextColor());
                    } else current = Text.builder(character + piece);
                } else current = Text.builder(piece);

                if (result != null) {
                    current.append(result);
                }
                result = current.build();
            }
            if (result != null) return result;
        }
        return Text.of(str);
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
