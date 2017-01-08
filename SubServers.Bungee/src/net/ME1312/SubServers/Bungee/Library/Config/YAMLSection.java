package net.ME1312.SubServers.Bungee.Library.Config;

import net.ME1312.SubServers.Bungee.Library.Util;
import net.md_5.bungee.api.ChatColor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * YAML Config Section Class
 */
@SuppressWarnings({"unchecked", "unused"})
public class YAMLSection {
    protected Map<String, Object> map;
    protected String handle = null;
    protected YAMLSection up = null;
    private Yaml yaml;

    /**
     * Creates an empty YAML Section
     */
    public YAMLSection() {
        this.map = new HashMap<>();
        this.yaml = new Yaml(YAMLConfig.getDumperOptions());
    }

    /**
     * Creates a YAML Section from an Input Stream
     *
     * @param stream Input Stream
     * @throws YAMLException
     */
    public YAMLSection(InputStream stream) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(stream);
    }

    /**
     * Creates a YAML Section from a Reader
     *
     * @param reader Reader
     * @throws YAMLException
     */
    public YAMLSection(Reader reader) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(reader);
    }

    /**
     * Creates a YAML Section from JSON Contents
     *
     * @param json JSON
     */
    public YAMLSection(JSONObject json) {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(json.toString(4));
    }

    /**
     * Creates a YAML Section from String
     *
     * @param str String
     * @throws YAMLException
     */
    public YAMLSection(String str) throws YAMLException {
        this.map = (Map<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).load(str);
    }
    
    protected YAMLSection(Map<String, ?> map, YAMLSection up, String handle, Yaml yaml) {
        this.map = new HashMap<String, Object>();
        this.yaml = yaml;
        this.handle = handle;
        this.up = up;

        if (map != null) {
            for (String key : map.keySet()) {
                this.map.put(key, map.get(key));
            }
        }
    }

    /**
     * Get the Keys
     *
     * @return KeySet
     */
    public Set<String> getKeys() {
        return map.keySet();
    }


    /**
     * Get the Values
     *
     * @return Values
     */
    public Collection<YAMLValue> getValues() {
        List<YAMLValue> values = new ArrayList<YAMLValue>();
        for (String value : map.keySet()) {
            values.add(new YAMLValue(map.get(value), this, value, yaml));
        }
        return values;
    }

    /**
     * Check if a Handle exists
     *
     * @param handle Handle
     * @return if that handle exists
     */
    public boolean contains(String handle) {
        return map.keySet().contains(handle);
    }

    /**
     * Remove an Object by Handle
     *
     * @param handle Handle
     */
    public void remove(String handle) {
        map.remove(handle);

        if (this.handle != null && this.up != null) {
            this.up.set(this.handle, this);
        }
    }

    /**
     * Remove all Objects from this YAML Section
     */
    public void clear() {
        map.clear();
    }

    private Object convert(Object value) {
        if (value instanceof JSONObject) {
            value = new YAMLSection((JSONObject) value);
        }

        if (value instanceof YAMLConfig) {
            ((YAMLConfig) value).get().up = this;
            ((YAMLConfig) value).get().handle = handle;
            return ((YAMLConfig) value).get().map;
        } else if (value instanceof YAMLSection) {
            ((YAMLSection) value).up = this;
            ((YAMLSection) value).handle = handle;
            return ((YAMLSection) value).map;
        } else if (value instanceof YAMLValue) {
            return ((YAMLValue) value).asObject();
        } else if (value instanceof JSONArray) {
            List<Object> list = new ArrayList<Object>();
            for (int i=0; i < ((JSONArray) value).length(); i++) list.add(((JSONArray) value).getString(i));
            return list;
        } else if (value instanceof UUID) {
            return ((UUID) value).toString();
        } else {
            return value;
        }
    }

    /**
     * Set Object into this YAML Section
     *
     * @param handle Handle
     * @param value Value
     */
    public void set(String handle, Object value) {
        if (value instanceof Collection) {
            set(handle, (Collection<?>) value);
        } else {
            map.put(handle, convert(value));

            if (this.handle != null && this.up != null) {
                this.up.set(this.handle, this);
            }
        }
    }

    /**
     * Set Collection&lt;V&gt; into this YAML Section
     *
     * @param handle Handle
     * @param list Value
     * @param <V> Collection Type
     */
    public <V> void set(String handle, Collection<V> list) {
        List<Object> values = new LinkedList<Object>();
        for (V value : list) {
            values.add(convert(value));
        }
        map.put(handle, values);

        if (this.handle != null && this.up != null) {
            this.up.set(this.handle, this);
        }
    }

    /**
     * Set All Objects into this YAML Section
     *
     * @param values Map to set
     */
    public void setAll(Map<String, ?> values) {
        for (String value : values.keySet()) {
            set(value, values.get(value));
        }
    }

    /**
     * Copy YAML Values to this YAML Section
     *
     * @param values
     */
    public void setAll(YAMLSection values) {
        for (String value : values.map.keySet()) {
            set(value, values.map.get(value));
        }
    }

    /**
     * Go up a level in the config (or null if this is the top layer)
     *
     * @return Super Section
     */
    public YAMLSection superSection() {
        return up;
    }

    /**
     * Clone this YAML Section
     *
     * @return
     */
    public YAMLSection clone() {
        return new YAMLSection(map, null, null, yaml);
    }

    @Override
    public String toString() {
        return yaml.dump(map);
    }

    /**
     * Convert to JSON
     *
     * @return JSON
     */
    public JSONObject toJSON() {
        return new JSONObject(map);
    }

    /**
     * Get an Object by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public YAMLValue get(String handle) {
        return (map.get(handle) != null)?(new YAMLValue(map.get(handle), this, handle, yaml)):null;
    }

    /**
     * Get an Object by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object
     */
    public YAMLValue get(String handle, Object def) {
        return new YAMLValue((map.get(handle) != null)?map.get(handle):def, this, handle, yaml);
    }

    /**
     * Get an Object by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object
     */
    public YAMLValue get(String handle, YAMLValue def) {
        return (map.get(handle) != null) ? (new YAMLValue(map.get(handle), this, handle, yaml)) : def;
    }

    /**
     * Get a List by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public List<YAMLValue> getList(String handle) {
        if (map.get(handle) != null) {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (Object value : (List<?>) map.get(handle)) {
                values.add(new YAMLValue(value, null, null, yaml));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * Get a List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object List
     */
    public List<YAMLValue> getList(String handle, Collection<?> def) {
        if (map.get(handle) != null) {
            return getList(handle);
        } else {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (Object value : def) {
                values.add(new YAMLValue(value, null, null, yaml));
            }
            return values;
        }
    }

    /**
     * Get a List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object List
     */
    public List<YAMLValue> getList(String handle, List<? extends YAMLValue> def) {
        if (map.get(handle) != null) {
            return getList(handle);
        } else {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (YAMLValue value : def) {
                values.add(value);
            }
            return values;
        }
    }

    /**
     * Get a Object by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public Object getObject(String handle) {
        return map.get(handle);
    }

    /**
     * Get a Object by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object
     */
    public Object getObject(String handle, Object def) {
        return (map.get(handle) != null)?map.get(handle):def;
    }

    /**
     * Get a Object List by Handle
     *
     * @param handle Handle
     * @return Object List
     */
    public List<?> getObjectList(String handle) {
        return (List<?>) map.get(handle);
    }

    /**
     * Get a Object List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object List
     */
    public List<?> getObjectList(String handle, List<?> def) {
        return (List<?>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Boolean by Handle
     *
     * @param handle Handle
     * @return Boolean
     */
    public boolean getBoolean(String handle) {
        return (boolean) map.get(handle);
    }

    /**
     * Get a Boolean by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Boolean
     */
    public boolean getBoolean(String handle, boolean def) {
        return (boolean) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Boolean List by Handle
     *
     * @param handle Handle
     * @return Boolean List
     */
    public List<Boolean> getBooleanList(String handle) {
        return (List<Boolean>) map.get(handle);
    }

    /**
     * Get a Boolean List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Boolean List
     */
    public List<Boolean> getBooleanList(String handle, List<Boolean> def) {
        return (List<Boolean>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @return YAML Section
     */
    public YAMLSection getSection(String handle) {
        return (map.get(handle) != null)?(new YAMLSection((Map<String, Object>) map.get(handle), this, handle, yaml)):null;
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section
     */
    public YAMLSection getSection(String handle, Map<String, ?> def) {
        return new YAMLSection((Map<String, Object>) ((map.get(handle) != null)?map.get(handle):def), this, handle, yaml);
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section
     */
    public YAMLSection getSection(String handle, YAMLSection def) {
        return (map.get(handle) != null)?(new YAMLSection((Map<String, Object>) map.get(handle), this, handle, yaml)):def;
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle) {
        if (map.get(handle) != null) {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (Map<String, ?> value : (List<? extends Map<String, ?>>) map.get(handle)) {
                values.add(new YAMLSection(value, null, null, yaml));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle, Collection<? extends Map<String, ?>> def) {
        if (map.get(handle) != null) {
            return getSectionList(handle);
        } else {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (Map<String, ?> value : def) {
                values.add(new YAMLSection(value, null, null, yaml));
            }
            return values;
        }
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle, List<? extends YAMLSection> def) {
        if (map.get(handle) != null) {
            return getSectionList(handle);
        } else {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (YAMLSection value : def) {
                values.add(value);
            }
            return values;
        }
    }

    /**
     * Get a Double by Handle
     *
     * @param handle Handle
     * @return Double
     */
    public double getDouble(String handle) {
        return (double) map.get(handle);
    }

    /**
     * Get a Double by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Double
     */
    public double getDouble(String handle, double def) {
        return (double) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Double List by Handle
     *
     * @param handle Handle
     * @return Double List
     */
    public List<Double> getDoubleList(String handle) {
        return (List<Double>) map.get(handle);
    }

    /**
     * Get a Double List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Double List
     */
    public List<Double> getDoubleList(String handle, List<Double> def) {
        return (List<Double>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Float by Handle
     *
     * @param handle Handle
     * @return Float
     */
    public float getFloat(String handle) {
        return (float) map.get(handle);
    }

    /**
     * Get a Float by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Float
     */
    public float getFloat(String handle, float def) {
        return (float) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Float List by Handle
     *
     * @param handle Handle
     * @return Float List
     */
    public List<Float> getFloatList(String handle) {
        return (List<Float>) map.get(handle);
    }

    /**
     * Get a Float List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Float List
     */
    public List<Float> getFloatList(String handle, float def) {
        return (List<Float>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get an Integer by Handle
     *
     * @param handle Handle
     * @return Integer
     */
    public int getInt(String handle) {
        return (int) map.get(handle);
    }

    /**
     * Get an Integer by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Integer
     */
    public int getInt(String handle, int def) {
        return (int) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get an Integer List by Handle
     *
     * @param handle Handle
     * @return Integer List
     */
    public List<Integer> getIntList(String handle) {
        return (List<Integer>) map.get(handle);
    }

    /**
     * Get an Integer List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Integer List
     */
    public List<Integer> getIntList(String handle, List<Integer> def) {
        return (List<Integer>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Long by Handle
     *
     * @param handle Handle
     * @return Long
     */
    public long getLong(String handle) {
        return (long) map.get(handle);
    }

    /**
     * Get a Long by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Long
     */
    public long getLong(String handle, long def) {
        return (long) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Long List by Handle
     *
     * @param handle Handle
     * @return Long List
     */
    public List<Long> getLongList(String handle) {
        return (List<Long>) map.get(handle);
    }

    /**
     * Get a Long List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Long List
     */
    public List<Long> getLongList(String handle, List<Long> def) {
        return (List<Long>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Short by Handle
     *
     * @param handle Handle
     * @return Short
     */
    public short getShort(String handle) {
        return (short) map.get(handle);
    }

    /**
     * Get a Short by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Short
     */
    public short getShort(String handle, short def) {
        return (short) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a Short List by Handle
     *
     * @param handle Handle
     * @return Short List
     */
    public List<Short> getShortList(String handle) {
        return (List<Short>) map.get(handle);
    }

    /**
     * Get a Short List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Short List
     */
    public List<Short> getShortList(String handle, List<Short> def) {
        return (List<Short>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get an Unparsed String by Handle
     *
     * @param handle Handle
     * @return Unparsed String
     */
    public String getRawString(String handle) {
        return (String) map.get(handle);
    }

    /**
     * Get an Unparsed String by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Unparsed String
     */
    public String getRawString(String handle, String def) {
        return (String) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get an Unparsed String List by Handle
     *
     * @param handle Handle
     * @return Unparsed String List
     */
    public List<String> getRawStringList(String handle) {
        return (List<String>) map.get(handle);
    }

    /**
     * Get an Unparsed String List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Unparsed String List
     */
    public List<String> getRawStringList(String handle, List<String> def) {
        return (List<String>) ((map.get(handle) != null)?map.get(handle):def);
    }

    /**
     * Get a String by Handle
     *
     * @param handle Handle
     * @return String
     */
    public String getString(String handle) {
        return (map.get(handle) != null)?Util.unescapeJavaString((String) map.get(handle)):null;
    }

    /**
     * Get a String by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return String
     */
    public String getString(String handle, String def) {
        return Util.unescapeJavaString((String) ((map.get(handle) != null) ? map.get(handle) : def));
    }

    /**
     * Get a String List by Handle
     *
     * @param handle Handle
     * @return String List
     */
    public List<String> getStringList(String handle) {
        if (map.get(handle) != null) {
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) map.get(handle)) {
                values.add(Util.unescapeJavaString(value));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * Get a String List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return String List
     */
    public List<String> getStringList(String handle, List<String> def) {
        if (map.get(handle) != null) {
            return getStringList(handle);
        } else {
            List<String> values = new ArrayList<String>();
            for (String value : def) {
                values.add(Util.unescapeJavaString(value));
            }
            return values;
        }
    }

    /**
     * Get a Colored String by Handle
     *
     * @param handle Handle
     * @param color Color Char to parse
     * @return Colored String
     */
    public String getColoredString(String handle, char color) {
        return (map.get(handle) != null)? ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString((String) map.get(handle))):null;
    }

    /**
     * Get a Colored String by Handle
     *
     * @param handle Handle
     * @param def Default
     * @param color Color Char to parse
     * @return Colored String
     */
    public String getColoredString(String handle, String def, char color) {
        return ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString((String) ((map.get(handle) != null) ? map.get(handle) : def)));
    }
    /**
     * Get a Colored String List by Handle
     *
     * @param handle Handle
     * @param color Color Char to parse
     * @return Colored String List
     */
    public List<String> getColoredStringList(String handle, char color) {
        if (map.get(handle) != null) {
            List<String> values = new ArrayList<String>();
            for (String value : (List<String>) map.get(handle)) {
                values.add(ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString(value)));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * Get a Colored String List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @param color Color Char to parse
     * @return Colored String List
     */
    public List<String> getColoredStringList(String handle, List<String> def, char color) {
        if (map.get(handle) != null) {
            return getColoredStringList(handle, color);
        } else {
            List<String> values = new ArrayList<String>();
            for (String value : def) {
                values.add(ChatColor.translateAlternateColorCodes(color, Util.unescapeJavaString(value)));
            }
            return values;
        }
    }

    /**
     * Get a UUID by Handle
     *
     * @param handle Handle
     * @return UUID
     */
    public UUID getUUID(String handle) {
        return (map.get(handle) != null)?UUID.fromString((String) map.get(handle)):null;
    }

    /**
     * Get a UUID by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return UUID
     */
    public UUID getUUID(String handle, UUID def) {
        return UUID.fromString((String) ((map.get(handle) != null) ? map.get(handle) : def));
    }

    /**
     * Get a UUID List by Handle
     *
     * @param handle Handle
     * @return UUID List
     */
    public List<UUID> getUUIDList(String handle) {
        if (map.get(handle) != null) {
            List<UUID> values = new ArrayList<UUID>();
            for (String value : (List<String>) map.get(handle)) {
                values.add(UUID.fromString(value));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * Get a UUID List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return UUID List
     */
    public List<UUID> getUUIDList(String handle, List<UUID> def) {
        if (map.get(handle) != null) {
            return getUUIDList(handle);
        } else {
            return def;
        }
    }

    /**
     * Check if object is a Boolean by Handle
     *
     * @param handle Handle
     * @return Object Boolean Status
     */
    public boolean isBoolean(String handle) {
        return (map.get(handle) instanceof Boolean);
    }

    /**
     * Check if object is a YAML Section by Handle
     *
     * @param handle Handle
     * @return Object YAML Section Status
     */
    public boolean isSection(String handle) {
        return (map.get(handle) instanceof Map);
    }

    /**
     * Check if object is a Double by Handle
     *
     * @param handle Handle
     * @return Object Double Status
     */
    public boolean isDouble(String handle) {
        return (map.get(handle) instanceof Double);
    }

    /**
     * Check if object is a Float by Handle
     *
     * @param handle Handle
     * @return Object Float Status
     */
    public boolean isFloat(String handle) {
        return (map.get(handle) instanceof Float);
    }

    /**
     * Check if object is an Integer by Handle
     *
     * @param handle Handle
     * @return Object Integer Status
     */
    public boolean isInt(String handle) {
        return (map.get(handle) instanceof Integer);
    }

    /**
     * Check if object is a List by Handle
     *
     * @param handle Handle
     * @return Object List Status
     */
    public boolean isList(String handle) {
        return (map.get(handle) instanceof List);
    }

    /**
     * Check if object is a Long by Handle
     *
     * @param handle Handle
     * @return Object Long Status
     */
    public boolean isLong(String handle) {
        return (map.get(handle) instanceof Long);
    }

    /**
     * Check if object is a String by Handle
     *
     * @param handle Handle
     * @return Object String Status
     */
    public boolean isString(String handle) {
        return (map.get(handle) instanceof String);
    }

    /**
     * Check if object is a UUID by Handle
     *
     * @param handle Handle
     * @return Object UUID Status
     */
    public boolean isUUID(String handle) {
        return (map.get(handle) instanceof String && !Util.isException(() -> UUID.fromString((String) map.get(handle))));
    }

}
