package net.ME1312.SubServers.Client.Bukkit.Library.Config;

import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.md_5.bungee.api.ChatColor;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * YAML Value Class
 */
@SuppressWarnings({"unchecked", "unused"})
public class YAMLValue {
    protected Object obj;
    protected String label;
    protected YAMLSection up;
    private Yaml yaml;

    protected YAMLValue(Object obj, YAMLSection up, String label, Yaml yaml) {
        this.obj = obj;
        this.label = label;
        this.yaml = yaml;
        this.up = up;
    }

    /**
     * Get the YAML Section this Object was defined in
     *
     * @return YAML Section
     */
    public YAMLSection getDefiningSection() {
        return up;
    }

    /**
     * Get Object
     *
     * @return Object
     */
    public Object asObject() {
        return obj;
    }

    /**
     * Get Object as List
     *
     * @return List
     */
    public List<?> asObjectList() {
        return (List<?>) obj;
    }

    /**
     * Get Object as Boolean
     *
     * @return Boolean
     */
    public boolean asBoolean() {
        return (boolean) obj;
    }

    /**
     * Get Object as List
     *
     * @return List
     */
    public List<Boolean> asBooleanList() {
        return (List<Boolean>) obj;
    }

    /**
     * Get Object as YAML Section
     *
     * @return YAML Section
     */
    public YAMLSection asSection() {
        return new YAMLSection((Map<String, ?>) obj, up, label, yaml);
    }

    /**
     * Get Object as YAML Section List
     *
     * @return YAML Section List
     */
    public List<YAMLSection> asSectionList() {
        List<YAMLSection> values = new ArrayList<YAMLSection>();
        for (Map<String, ?> value : (List<? extends Map<String, ?>>) obj) {
            values.add(new YAMLSection(value, null, null, yaml));
        }
        return values;
    }

    /**
     * Get Object as Double
     *
     * @return Double
     */
    public double asDouble() {
        return (double) obj;
    }

    /**
     * Get Object as Double List
     *
     * @return Double List
     */
    public List<Double> asDoubleList() {
        return (List<Double>) obj;
    }

    /**
     * Get Object as Float
     *
     * @return Float
     */
    public float asFloat() {
        return (float) obj;
    }

    /**
     * Get Object as Float List
     *
     * @return Float List
     */
    public List<Float> asFloatList() {
        return (List<Float>) obj;
    }

    /**
     * Get Object as Integer
     *
     * @return Integer
     */
    public int asInt() {
        return (int) obj;
    }

    /**
     * Get Object as Integer List
     *
     * @return Integer List
     */
    public List<Integer> asIntList() {
        return (List<Integer>) obj;
    }

    /**
     * Get Object as Long
     *
     * @return Long
     */
    public long asLong() {
        return (long) obj;
    }

    /**
     * Get Object as Long List
     *
     * @return Long List
     */
    public List<Long> asLongList() {
        return (List<Long>) obj;
    }

    /**
     * Get Object as Unparsed String
     *
     * @return Unparsed String
     */
    public String asRawString() {
        return (String) obj;
    }

    /**
     * Get Object as Unparsed String List
     *
     * @return Unparsed String List
     */
    public List<String> asRawStringList() {
        return (List<String>) obj;
    }

    /**
     * Get Object as String
     *
     * @return String
     */
    public String asString() {
        return Util.unescapeJavaString((String) obj);
    }

    /**
     * Get Object as String List
     *
     * @return String List
     */
    public List<String> asStringList() {
        List<String> values = new ArrayList<String>();
        for (String value : (List<String>) obj) {
            values.add(Util.unescapeJavaString(value));
        }
        return values;
    }

    /**
     * Get Object as Colored String
     *
     * @param color Color Char to parse
     * @return Colored String
     */
    public String asColoredString(char color) {
        if (Util.isNull(color)) throw new NullPointerException();
        return ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString((String) obj));
    }

    /**
     * Get Object as Colored String List
     *
     * @param color Color Char to parse
     * @return Colored String List
     */
    public List<String> asColoredStringList(char color) {
        if (Util.isNull(color)) throw new NullPointerException();
        List<String> values = new ArrayList<String>();
        for (String value : (List<String>) obj) {
            values.add(ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString(value)));
        }
        return values;
    }

    /**
     * Get Object as UUID
     *
     * @return UUID
     */
    public UUID asUUID() {
        return UUID.fromString((String) obj);
    }

    public List<UUID> asUUIDList() {
        List<UUID> values = new ArrayList<UUID>();
        for (String value : (List<String>) obj) {
            values.add(UUID.fromString(value));
        }
        return values;
    }

    /**
     * Check if object is a Boolean
     *
     * @return Boolean Status
     */
    public boolean isBoolean() {
        return (obj instanceof Boolean);
    }

    /**
     * Check if object is a YAML Section
     *
     * @return YAML Section Status
     */
    public boolean isSection() {
        return (obj instanceof Map);
    }

    /**
     * Check if object is a Double
     *
     * @return Double Status
     */
    public boolean isDouble() {
        return (obj instanceof Double);
    }

    /**
     * Check if object is a Float
     *
     * @return Float Status
     */
    public boolean isFloat() {
        return (obj instanceof Float);
    }

    /**
     * Check if object is an Integer
     *
     * @return Integer Status
     */
    public boolean isInt() {
        return (obj instanceof Integer);
    }

    /**
     * Check if object is a List
     *
     * @return List Status
     */
    public boolean isList() {
        return (obj instanceof List);
    }

    /**
     * Check if object is a Long
     *
     * @return Long Status
     */
    public boolean isLong() {
        return (obj instanceof Long);
    }

    /**
     * Check if object is a String
     *
     * @return String Status
     */
    public boolean isString() {
        return (obj instanceof String);
    }

    /**
     * Check if object is a UUID
     *
     * @return UUID Status
     */
    public boolean isUUID() {
        return (obj instanceof String && !Util.isException(() -> UUID.fromString((String) obj)));
    }

    @Override
    public String toString() {
        return obj.toString();
    }
}
