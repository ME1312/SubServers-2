package net.ME1312.SubServers.Client.Bukkit.Library.Config;

import org.bukkit.ChatColor;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class YAMLSection {
    protected Map<String, Object> map;
    protected String label = null;
    protected YAMLSection up = null;
    private Yaml yaml;

    public YAMLSection() {
        this.map = new HashMap<>();
        this.yaml = new Yaml(YAMLConfig.getDumperOptions());
    }

    public YAMLSection(InputStream io) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(io);
    }

    public YAMLSection(Reader reader) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(reader);
    }

    public YAMLSection(JSONObject json) {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(json.toString(4));
    }

    public YAMLSection(String yaml) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(yaml);
    }
    
    protected YAMLSection(Map<String, ?> map, YAMLSection up, String label, Yaml yaml) {
        this.map = new HashMap<String, Object>();
        this.yaml = yaml;
        this.label = label;
        this.up = up;

        if (map != null) {
            for (String key : map.keySet()) {
                this.map.put(key, map.get(key));
            }
        }
    }

    public Set<String> getKeys() {
        return map.keySet();
    }


    public Collection<YAMLValue> getValues() {
        List<YAMLValue> values = new ArrayList<YAMLValue>();
        for (String value : map.keySet()) {
            values.add(new YAMLValue(map.get(value), this, value, yaml));
        }
        return values;
    }

    public boolean contains(String label) {
        return map.keySet().contains(label);
    }

    public void remove(String label) {
        map.remove(label);
    }

    public void clear() {
        map.clear();
    }

    public void set(String label, Object value) {
        if (value instanceof YAMLConfig) { // YAML Handler Values
            ((YAMLConfig) value).get().up = this;
            ((YAMLConfig) value).get().label = label;
            map.put(label, ((YAMLConfig) value).get().map);
        } else if (value instanceof YAMLSection) {
            ((YAMLSection) value).up = this;
            ((YAMLSection) value).label = label;
            map.put(label, ((YAMLSection) value).map);
        } else if (value instanceof YAMLValue) {
            map.put(label, ((YAMLValue) value).asObject());
        } else if (value instanceof UUID) {
            map.put(label, ((UUID) value).toString());
        } else {
            map.put(label, value);
        }

        if (this.label != null && this.up != null) {
            this.up.set(this.label, this);
        }
    }

    public void setAll(Map<String, ?> values) {
        for (String value : values.keySet()) {
            set(value, values.get(value));
        }
    }

    public void setAll(YAMLSection values) {
        for (String value : values.map.keySet()) {
            set(value, values.map.get(value));
        }
    }

    public YAMLSection superSection() {
        return up;
    }

    @Override
    public String toString() {
        return yaml.dump(map);
    }

    public JSONObject toJSON() {
        return new JSONObject(map);
    }

    public YAMLValue get(String label) {
        return (map.get(label) != null)?(new YAMLValue(map.get(label), this, label, yaml)):null;
    }

    public YAMLValue get(String label, Object def) {
        return new YAMLValue((map.get(label) != null)?map.get(label):def, this, label, yaml);
    }

    public YAMLValue get(String label, YAMLValue def) {
        return (map.get(label) != null) ? (new YAMLValue(map.get(label), this, label, yaml)) : def;
    }

    public List<YAMLValue> getList(String label) {
        if (map.get(label) != null) {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (Object value : (List<?>) map.get(label)) {
                values.add(new YAMLValue(value, null, null, yaml));
            }
            return values;
        } else {
            return null;
        }
    }

    public List<YAMLValue> getList(String label, Collection<?> def) {
        if (map.get(label) != null) {
            return getList(label);
        } else {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (Object value : def) {
                values.add(new YAMLValue(value, null, null, yaml));
            }
            return values;
        }
    }

    public List<YAMLValue> getList(String label, List<? extends YAMLValue> def) {
        if (map.get(label) != null) {
            return getList(label);
        } else {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (YAMLValue value : def) {
                values.add(value);
            }
            return values;
        }
    }

    public Object getObject(String label) {
        return map.get(label);
    }

    public Object getObject(String label, Object def) {
        return (map.get(label) != null)?map.get(label):def;
    }

    public List<?> getObjectList(String label) {
        return (List<?>) map.get(label);
    }

    public List<?> getObjectList(String label, List<?> def) {
        return (List<?>) ((map.get(label) != null)?map.get(label):def);
    }

    public boolean getBoolean(String label) {
        return (boolean) map.get(label);
    }

    public boolean getBoolean(String label, boolean def) {
        return (boolean) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Boolean> getBooleanList(String label) {
        return (List<Boolean>) map.get(label);
    }

    public List<Boolean> getBooleanList(String label, List<Boolean> def) {
        return (List<Boolean>) ((map.get(label) != null)?map.get(label):def);
    }

    public YAMLSection getSection(String label) {
        return (map.get(label) != null)?(new YAMLSection((Map<String, Object>) map.get(label), this, label, yaml)):null;
    }

    public YAMLSection getSection(String label, Map<String, ?> def) {
        return new YAMLSection((Map<String, Object>) ((map.get(label) != null)?map.get(label):def), this, label, yaml);
    }

    public YAMLSection getSection(String label, YAMLSection def) {
        return (map.get(label) != null)?(new YAMLSection((Map<String, Object>) map.get(label), this, label, yaml)):def;
    }

    public List<YAMLSection> getSectionList(String label) {
        if (map.get(label) != null) {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (Map<String, ?> value : (List<? extends Map<String, ?>>) map.get(label)) {
                values.add(new YAMLSection(value, null, null, yaml));
            }
            return values;
        } else {
            return null;
        }
    }

    public List<YAMLSection> getSectionList(String label, Collection<? extends Map<String, ?>> def) {
        if (map.get(label) != null) {
            return getSectionList(label);
        } else {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (Map<String, ?> value : def) {
                values.add(new YAMLSection(value, null, null, yaml));
            }
            return values;
        }
    }

    public List<YAMLSection> getSectionList(String label, List<? extends YAMLSection> def) {
        if (map.get(label) != null) {
            return getSectionList(label);
        } else {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (YAMLSection value : def) {
                values.add(value);
            }
            return values;
        }
    }

    public double getDouble(String label) {
        return (double) map.get(label);
    }

    public double getDouble(String label, double def) {
        return (double) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Double> getDoubleList(String label) {
        return (List<Double>) map.get(label);
    }

    public List<Double> getDoubleList(String label, List<Double> def) {
        return (List<Double>) ((map.get(label) != null)?map.get(label):def);
    }

    public float getFloat(String label) {
        return (float) map.get(label);
    }

    public float getFloat(String label, float def) {
        return (float) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Float> getFloatList(String label) {
        return (List<Float>) map.get(label);
    }

    public List<Float> getFloatList(String label, float def) {
        return (List<Float>) ((map.get(label) != null)?map.get(label):def);
    }

    public int getInt(String label) {
        return (int) map.get(label);
    }

    public int getInt(String label, int def) {
        return (int) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Integer> getIntList(String label) {
        return (List<Integer>) map.get(label);
    }

    public List<Integer> getIntList(String label, List<Integer> def) {
        return (List<Integer>) ((map.get(label) != null)?map.get(label):def);
    }

    public long getLong(String label) {
        return (long) map.get(label);
    }

    public long getLong(String label, long def) {
        return (long) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Long> getLongList(String label) {
        return (List<Long>) map.get(label);
    }

    public List<Long> getLongList(String label, List<Long> def) {
        return (List<Long>) ((map.get(label) != null)?map.get(label):def);
    }

    public short getShort(String label) {
        return (short) map.get(label);
    }

    public short getShort(String label, short def) {
        return (short) ((map.get(label) != null)?map.get(label):def);
    }

    public List<Short> getShortList(String label) {
        return (List<Short>) map.get(label);
    }

    public List<Short> getShortList(String label, List<Short> def) {
        return (List<Short>) ((map.get(label) != null)?map.get(label):def);
    }

    public String getRawString(String label) {
        return (String) map.get(label);
    }

    public String getRawString(String label, String def) {
        return (String) ((map.get(label) != null)?map.get(label):def);
    }

    public List<String> getRawStringList(String label) {
        return (List<String>) map.get(label);
    }

    public List<String> getRawStringList(String label, List<String> def) {
        return (List<String>) ((map.get(label) != null)?map.get(label):def);
    }

    public String getString(String label) {
        return (map.get(label) != null)?unescapeJavaString((String) map.get(label)):null;
    }

    public String getString(String label, String def) {
        return unescapeJavaString((String) ((map.get(label) != null) ? map.get(label) : def));
    }

    public List<String> getStringList(String label) {
        if (map.get(label) != null) {
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) map.get(label)) {
                values.add(unescapeJavaString(value));
            }
            return values;
        } else {
            return null;
        }
    }

    public List<String> getStringList(String label, List<String> def) {
        if (map.get(label) != null) {
            return getStringList(label);
        } else {
            List<String> values = new ArrayList<String>();
            for (String value : def) {
                values.add(unescapeJavaString(value));
            }
            return values;
        }
    }

    public String getColoredString(String label, char color) {
        return (map.get(label) != null)? ChatColor.translateAlternateColorCodes(color, unescapeJavaString((String) map.get(label))):null;
    }

    public String getColoredString(String label, String def, char color) {
        return ChatColor.translateAlternateColorCodes(color, unescapeJavaString((String) ((map.get(label) != null) ? map.get(label) : def)));
    }

    public List<String> getColoredStringList(String label, char color) {
        if (map.get(label) != null) {
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) map.get(label)) {
                values.add(ChatColor.translateAlternateColorCodes(color, unescapeJavaString(value)));
            }
            return values;
        } else {
            return null;
        }
    }

    public List<String> getColoredStringList(String label, List<String> def, char color) {
        if (map.get(label) != null) {
            return getColoredStringList(label, color);
        } else {
            List<String> values = new ArrayList<String>();
            for (String value : def) {
                values.add(ChatColor.translateAlternateColorCodes(color, unescapeJavaString(value)));
            }
            return values;
        }
    }

    public List<UUID> getUUIDList(String label) {
        if (map.get(label) != null) {
            List<UUID> values = new ArrayList<UUID>();
            for (String value : (List<String>) map.get(label)) {
                values.add(UUID.fromString(value));
            }
            return values;
        } else {
            return null;
        }
    }

    public List<UUID> getUUIDList(String label, List<UUID> def) {
        if (map.get(label) != null) {
            return getUUIDList(label);
        } else {
            return def;
        }
    }

    public boolean isBoolean(String label) {
        return (map.get(label) instanceof Boolean);
    }

    public boolean isSection(String label) {
        return (map.get(label) instanceof Map);
    }

    public boolean isDouble(String label) {
        return (map.get(label) instanceof Double);
    }

    public boolean isFloat(String label) {
        return (map.get(label) instanceof Float);
    }

    public boolean isInt(String label) {
        return (map.get(label) instanceof Integer);
    }

    public boolean isList(String label) {
        return (map.get(label) instanceof List);
    }

    public boolean isLong(String label) {
        return (map.get(label) instanceof Long);
    }

    public boolean isString(String label) {
        return (map.get(label) instanceof String);
    }

    static String unescapeJavaString(String str) {

        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == str.length() - 1) ? '\\' : str
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                            && str.charAt(i + 1) <= '7') {
                        code += str.charAt(i + 1);
                        i++;
                        if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                                && str.charAt(i + 1) <= '7') {
                            code += str.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= str.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + str.charAt(i + 2) + str.charAt(i + 3)
                                        + str.charAt(i + 4) + str.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
