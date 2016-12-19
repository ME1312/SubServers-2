package net.ME1312.SubServers.Client.Bukkit.Library.Config;

import org.bukkit.ChatColor;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "unused"})
public class YAMLValue {
    protected Object obj;
    protected String label;
    protected YAMLSection up;
    private Yaml yaml;

    public YAMLValue(Object obj, YAMLSection up, String label, Yaml yaml) {
        this.obj = obj;
        this.label = label;
        this.yaml = yaml;
        this.up = up;
    }

    public YAMLSection getDefiningSection() {
        return up;
    }

    public Object asObject() {
        return obj;
    }

    public List<?> asObjectList() {
        return (List<?>) obj;
    }

    public boolean asBoolean() {
        return (boolean) obj;
    }

    public List<Boolean> asBooleanList() {
        return (List<Boolean>) obj;
    }

    public YAMLSection asSection() {
        return new YAMLSection((Map<String, ?>) obj, up, label, yaml);
    }

    public List<YAMLSection> asSectionList() {
        List<YAMLSection> values = new ArrayList<YAMLSection>();
        for (Map<String, ?> value : (List<? extends Map<String, ?>>) obj) {
            values.add(new YAMLSection(value, null, null, yaml));
        }
        return values;
    }

    public double asDouble() {
        return (double) obj;
    }

    public List<Double> asDoubleList() {
        return (List<Double>) obj;
    }

    public float asFloat() {
        return (float) obj;
    }

    public List<Float> asFloatList() {
        return (List<Float>) obj;
    }

    public int asInt() {
        return (int) obj;
    }

    public List<Integer> asIntList() {
        return (List<Integer>) obj;
    }

    public long asLong() {
        return (long) obj;
    }

    public List<Long> asLongList() {
        return (List<Long>) obj;
    }

    public String asRawString() {
        return (String) obj;
    }

    public List<String> asRawStringList() {
        return (List<String>) obj;
    }

    public String asString() {
        return YAMLSection.unescapeJavaString((String) obj);
    }

    public List<String> asStringList() {
        List<String> values = new ArrayList<String>();
        for (String value : (List<String>) obj) {
            values.add(YAMLSection.unescapeJavaString(value));
        }
        return values;
    }

    public String asColoredString(char color) {
        return ChatColor.translateAlternateColorCodes(color, YAMLSection.unescapeJavaString((String) obj));
    }

    public List<String> asColoredStringList(char color) {
        List<String> values = new ArrayList<String>();
        for (String value : (List<String>) obj) {
            values.add(ChatColor.translateAlternateColorCodes(color, YAMLSection.unescapeJavaString(value)));
        }
        return values;
    }

    public boolean isBoolean() {
        return (obj instanceof Boolean);
    }

    public boolean isSection() {
        return (obj instanceof Map);
    }

    public boolean isDouble() {
        return (obj instanceof Double);
    }

    public boolean isFloat(String path) {
        return (obj instanceof Float);
    }

    public boolean isInt() {
        return (obj instanceof Integer);
    }

    public boolean isList() {
        return (obj instanceof List);
    }

    public boolean isLong() {
        return (obj instanceof Long);
    }

    public boolean isString() {
        return (obj instanceof String);
    }

    @Override
    public String toString() {
        return obj.toString();
    }
}
