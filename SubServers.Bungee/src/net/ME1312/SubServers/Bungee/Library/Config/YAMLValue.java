package net.ME1312.SubServers.Bungee.Library.Config;

import net.ME1312.SubServers.Bungee.Library.Util;
import net.md_5.bungee.api.ChatColor;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

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
    public Boolean asBoolean() {
        return (Boolean) obj;
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
        if (obj != null) return new YAMLSection((Map<String, ?>) obj, up, label, yaml);
        else return null;
    }

    /**
     * Get Object as YAML Section List
     *
     * @return YAML Section List
     */
    public List<YAMLSection> asSectionList() {
        if (obj != null) {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (Map<String, ?> value : (List<? extends Map<String, ?>>) obj) {
                values.add(new YAMLSection(value, null, null, yaml));
            }
            return values;
        } else return null;
    }

    /**
     * Get Object as Double
     *
     * @return Double
     */
    public Double asDouble() {
        return (Double) obj;
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
    public Float asFloat() {
        return (Float) obj;
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
    public Integer asInt() {
        return (Integer) obj;
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
    public Long asLong() {
        return (Long) obj;
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
     * Get a Short by Handle
     *
     * @return Short
     */
    public Short asShort() {
        return (Short) obj;
    }

    /**
     * Get a Short List by Handle
     *
     * @return Short List
     */
    public List<Short> asShortList() {
        return (List<Short>) obj;
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
        if (obj != null) return Util.unescapeJavaString((String) obj);
        else return null;
    }

    /**
     * Get Object as String List
     *
     * @return String List
     */
    public List<String> asStringList() {
        if (obj != null) {
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) obj) {
                values.add(Util.unescapeJavaString(value));
            }
            return values;
        } else return null;
    }

    /**
     * Get Object as Colored String
     *
     * @param color Color Char to parse
     * @return Colored String
     */
    public String asColoredString(char color) {
        if (Util.isNull(color)) throw new NullPointerException();
        if (obj != null) return ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString((String) obj));
        else return null;
    }

    /**
     * Get Object as Colored String List
     *
     * @param color Color Char to parse
     * @return Colored String List
     */
    public List<String> asColoredStringList(char color) {
        if (obj != null) {
            if (Util.isNull(color)) throw new NullPointerException();
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) obj) {
                values.add(ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString(value)));
            }
            return values;
        } else return null;
    }

    /**
     * Get Object as UUID
     *
     * @return UUID
     */
    public UUID asUUID() {
        if (obj != null) return UUID.fromString((String) obj);
        else return null;
    }

    /**
     * Get Object as UUID List
     *
     * @return UUID List
     */
    public List<UUID> asUUIDList() {
        if (obj != null) {
            List<UUID> values = new ArrayList<UUID>();
            for (String value : (List<String>) obj) {
                values.add(UUID.fromString(value));
            }
            return values;
        } else return null;
    }


    /**
     * Check if object is Null
     *
     * @return Null Status
     */
    public boolean isNull() {
        return obj == null;
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
     * Check if object is a List
     *
     * @return List Status
     */
    public boolean isList() {
        return (obj instanceof List);
    }

    /**
     * Check if object is a Number
     *
     * @return Number Status
     */
    public boolean isNumber() {
        return (obj instanceof Number);
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
        if (obj != null) return obj.toString();
        else return "null";
    }
}
