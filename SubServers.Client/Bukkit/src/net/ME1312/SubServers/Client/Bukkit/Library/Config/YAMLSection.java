package net.ME1312.SubServers.Client.Bukkit.Library.Config;

import net.ME1312.SubServers.Client.Bukkit.Library.Util;
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
    protected LinkedHashMap<String, Object> map;
    protected String handle = null;
    protected YAMLSection up = null;
    private Yaml yaml;

    /**
     * Creates an empty YAML Section
     */
    public YAMLSection() {
        this.map = new LinkedHashMap<>();
        this.yaml = new Yaml(YAMLConfig.getDumperOptions());
    }

    /**
     * Creates a YAML Section from an Input Stream
     *
     * @param stream Input Stream
     * @throws YAMLException
     */
    public YAMLSection(InputStream stream) throws YAMLException {
        if (Util.isNull(stream)) throw new NullPointerException();
        this.map = (LinkedHashMap<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).loadAs(stream, LinkedHashMap.class);
    }

    /**
     * Creates a YAML Section from a Reader
     *
     * @param reader Reader
     * @throws YAMLException
     */
    public YAMLSection(Reader reader) throws YAMLException {
        if (Util.isNull(reader)) throw new NullPointerException();
        this.map = (LinkedHashMap<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).loadAs(reader, LinkedHashMap.class);
    }

    /**
     * Creates a YAML Section from String
     *
     * @param str String
     * @throws YAMLException
     */
    public YAMLSection(String str) throws YAMLException {
        if (Util.isNull(str)) throw new NullPointerException();
        this.map = (LinkedHashMap<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).loadAs(str, LinkedHashMap.class);
    }

    /**
     * Creates a YAML Section from JSON Contents
     *
     * @param json JSON
     */
    public YAMLSection(JSONObject json) {
        if (Util.isNull(json)) throw new NullPointerException();
        this.map = (LinkedHashMap<String, Object>) (this.yaml = new Yaml(YAMLConfig.getDumperOptions())).loadAs(json.toString(4), LinkedHashMap.class);
    }

    /**
     * Creates a YAML Section from Map Contents
     *
     * @param map Map
     */
    public YAMLSection(Map<String, ?> map) {
        if (Util.isNull(map)) throw new NullPointerException();
        this.map = new LinkedHashMap<>();
        this.yaml = new Yaml(YAMLConfig.getDumperOptions());

        setAll(map);
    }

    protected YAMLSection(Map<String, ?> map, YAMLSection up, String handle, Yaml yaml) {
        this.map = new LinkedHashMap<String, Object>();
        this.yaml = yaml;
        this.handle = handle;
        this.up = up;

        if (map != null) setAll(map);
    }


    /**
     * Get a copy of the original Object Map
     *
     * @return Object Map
     */
    public Map<String, ?> get() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.putAll(this.map);
        return map;
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

    private Object convert(Object value) {
        if (value instanceof JSONObject) {
            value = new YAMLSection((JSONObject) value);
        }

        if (value instanceof Map) {
            List<String> list = new ArrayList<String>();
            list.addAll(((Map<String, Object>) value).keySet());
            for (String key : list) ((Map<String, Object>) value).put(key, convert(((Map<String, Object>) value).get(key)));
            return value;
        } else if (value instanceof YAMLConfig) {
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
            for (int i = 0; i < ((JSONArray) value).length(); i++) list.add(convert(((JSONArray) value).get(i)));
            return list;
        } else if (value instanceof Collection) {
            List<Object> list = new ArrayList<Object>();
            for (Object val : (Collection<Object>) value) list.add(convert(val));
            return list;
        } else if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < ((Object[]) value).length; i++) list.add(convert(((Object[]) value)[i]));
            return list;
        } else if (value instanceof UUID) {
            return value.toString();
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
        if (Util.isNull(handle)) throw new NullPointerException();
        if (value == null) {
            remove(handle);
        } else if (value instanceof Collection) {
            set(handle, (Collection<?>) value);
        } else if (value.getClass().isArray()) {
            set(handle, (Object[]) value);
        } else {
            map.put(handle, convert(value));

            if (this.handle != null && this.up != null) {
                this.up.set(this.handle, this);
            }
        }
    }

    /**
     * Set Object into this YAML Section without overwriting existing value
     *
     * @param handle Handle
     * @param value Value
     */
    public void safeSet(String handle, Object value) {
        if (Util.isNull(handle)) throw new NullPointerException();
        if (!contains(handle)) set(handle, value);
    }

    /**
     * Set V[] into this YAML Section
     *
     * @param handle Handle
     * @param array Value
     * @param <V> Array Type
     */
    public <V> void set(String handle, V[] array) {
        if (Util.isNull(handle, array)) throw new NullPointerException();
        List<Object> values = new LinkedList<Object>();
        for (V value : array) {
            values.add(convert(value));
        }
        map.put(handle, values);

        if (this.handle != null && this.up != null) {
            this.up.set(this.handle, this);
        }
    }

    /**
     * Set V[] into this YAML Section without overwriting existing value
     *
     * @param handle Handle
     * @param array Value
     * @param <V> Array Type
     */
    public <V> void safeSet(String handle, V[] array) {
        if (Util.isNull(handle)) throw new NullPointerException();
        if (!contains(handle)) set(handle, array);
    }

    /**
     * Set Collection&lt;V&gt; into this YAML Section
     *
     * @param handle Handle
     * @param list Value
     * @param <V> Collection Type
     */
    public <V> void set(String handle, Collection<V> list) {
        if (Util.isNull(handle, list)) throw new NullPointerException();
        set(handle, list.toArray());
    }

    /**
     * Set Collection&lt;V&gt; into this YAML Section without overwriting existing value
     *
     * @param handle Handle
     * @param list Value
     * @param <V> Collection Type
     */
    public <V> void safeSet(String handle, Collection<V> list) {
        if (Util.isNull(handle)) throw new NullPointerException();
        if (!contains(handle)) set(handle, list);
    }

    /**
     * Set All Objects into this YAML Section
     *
     * @param values Map to set
     */
    public void setAll(Map<String, ?> values) {
        if (Util.isNull(values)) throw new NullPointerException();
        for (String value : values.keySet()) {
            set(value, values.get(value));
        }
    }

    /**
     * Set All Objects into this YAML Section without overwriting existing values
     *
     * @param values Map to set
     */
    public void safeSetAll(Map<String, ?> values) {
        if (Util.isNull(values)) throw new NullPointerException();
        for (String value : values.keySet()) {
            safeSet(value, values.get(value));
        }
    }

    /**
     * Copy YAML Values to this YAML Section
     *
     * @param values YAMLSection to merge
     */
    public void setAll(YAMLSection values) {
        if (Util.isNull(values)) throw new NullPointerException();
        setAll(values.map);
    }

    /**
     * Copy YAML Values to this YAML Section without overwriting existing values
     *
     * @param values YAMLSection to merge
     */
    public void safeSetAll(YAMLSection values) {
        if (Util.isNull(values)) throw new NullPointerException();
        safeSetAll(values.map);
    }

    /**
     * Remove an Object by Handle
     *
     * @param handle Handle
     */
    public void remove(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
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

    /**
     * Clone this YAML Section
     *
     * @return
     */
    public YAMLSection clone() {
        return new YAMLSection(map, null, null, yaml);
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
     * Get an Object by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public YAMLValue get(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
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
        if (Util.isNull(handle)) throw new NullPointerException();
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
        if (Util.isNull(handle)) throw new NullPointerException();
        return new YAMLValue((map.get(handle) != null)?map.get(handle):def.asObject(), this, handle, yaml);
    }

    /**
     * Get a List by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public List<YAMLValue> getList(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
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
        if (Util.isNull(handle)) throw new NullPointerException();
        if (map.get(handle) != null) {
            return getList(handle);
        } else if (def != null) {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (Object value : def) {
                values.add(new YAMLValue(value, null, null, yaml));
            }
            return values;
        } else return null;
    }

    /**
     * Get a List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object List
     */
    public List<YAMLValue> getList(String handle, List<? extends YAMLValue> def) {
        if (Util.isNull(handle)) throw new NullPointerException();
        if (map.get(handle) != null) {
            return getList(handle);
        } else if (def != null) {
            List<YAMLValue> values = new ArrayList<YAMLValue>();
            for (YAMLValue value : def) {
                values.add(new YAMLValue(value.asObject(), null, null, yaml));
            }
            return values;
        } else return null;
    }

    /**
     * Get a Object by Handle
     *
     * @param handle Handle
     * @return Object
     */
    public Object getObject(String handle) {
        return get(handle).asObject();
    }

    /**
     * Get a Object by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object
     */
    public Object getObject(String handle, Object def) {
        return get(handle, def).asObject();
    }

    /**
     * Get a Object List by Handle
     *
     * @param handle Handle
     * @return Object List
     */
    public List<?> getObjectList(String handle) {
        return get(handle).asObjectList();
    }

    /**
     * Get a Object List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Object List
     */
    public List<?> getObjectList(String handle, List<?> def) {
        return get(handle, def).asObjectList();
    }

    /**
     * Get a Boolean by Handle
     *
     * @param handle Handle
     * @return Boolean
     */
    public Boolean getBoolean(String handle) {
        return get(handle).asBoolean();
    }

    /**
     * Get a Boolean by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Boolean
     */
    public Boolean getBoolean(String handle, Boolean def) {
        return get(handle, def).asBoolean();
    }

    /**
     * Get a Boolean List by Handle
     *
     * @param handle Handle
     * @return Boolean List
     */
    public List<Boolean> getBooleanList(String handle) {
        return get(handle).asBooleanList();
    }

    /**
     * Get a Boolean List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Boolean List
     */
    public List<Boolean> getBooleanList(String handle, List<Boolean> def) {
        return get(handle, def).asBooleanList();
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @return YAML Section
     */
    public YAMLSection getSection(String handle) {
        return get(handle).asSection();
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section
     */
    public YAMLSection getSection(String handle, Map<String, ?> def) {
        return get(handle, def).asSection();
    }

    /**
     * Get a YAML Section by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section
     */
    public YAMLSection getSection(String handle, YAMLSection def) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return (map.get(handle) != null)?get(handle).asSection():((def != null)?new YAMLSection(def.get(), this, handle, yaml):null);
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle) {
        return get(handle).asSectionList();
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle, Collection<? extends Map<String, ?>> def) {
        return get(handle, def).asSectionList();
    }

    /**
     * Get a YAML Section List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return YAML Section List
     */
    public List<YAMLSection> getSectionList(String handle, List<? extends YAMLSection> def) {
        if (Util.isNull(handle)) throw new NullPointerException();
        if (map.get(handle) != null) {
            return get(handle).asSectionList();
        } else if (def != null) {
            List<YAMLSection> values = new ArrayList<YAMLSection>();
            for (YAMLSection value : def) {
                values.add(new YAMLSection(value.get(), null, null, yaml));
            }
            return values;
        } else return null;
    }

    /**
     * Get a Double by Handle
     *
     * @param handle Handle
     * @return Double
     */
    public Double getDouble(String handle) {
        return get(handle).asDouble();
    }

    /**
     * Get a Double by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Double
     */
    public Double getDouble(String handle, Double def) {
        return get(handle, def).asDouble();
    }

    /**
     * Get a Double List by Handle
     *
     * @param handle Handle
     * @return Double List
     */
    public List<Double> getDoubleList(String handle) {
        return get(handle).asDoubleList();
    }

    /**
     * Get a Double List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Double List
     */
    public List<Double> getDoubleList(String handle, List<Double> def) {
        return get(handle, def).asDoubleList();
    }

    /**
     * Get a Float by Handle
     *
     * @param handle Handle
     * @return Float
     */
    public Float getFloat(String handle) {
        return get(handle).asFloat();
    }

    /**
     * Get a Float by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Float
     */
    public Float getFloat(String handle, Float def) {
        return get(handle, def).asFloat();
    }

    /**
     * Get a Float List by Handle
     *
     * @param handle Handle
     * @return Float List
     */
    public List<Float> getFloatList(String handle) {
        return get(handle).asFloatList();
    }

    /**
     * Get a Float List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Float List
     */
    public List<Float> getFloatList(String handle, List<Float> def) {
        return get(handle, def).asFloatList();
    }

    /**
     * Get an Integer by Handle
     *
     * @param handle Handle
     * @return Integer
     */
    public Integer getInt(String handle) {
        return get(handle).asInt();
    }

    /**
     * Get an Integer by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Integer
     */
    public Integer getInt(String handle, Integer def) {
        return get(handle, def).asInt();
    }

    /**
     * Get an Integer List by Handle
     *
     * @param handle Handle
     * @return Integer List
     */
    public List<Integer> getIntList(String handle) {
        return get(handle).asIntList();
    }

    /**
     * Get an Integer List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Integer List
     */
    public List<Integer> getIntList(String handle, List<Integer> def) {
        return get(handle, def).asIntList();
    }

    /**
     * Get a Long by Handle
     *
     * @param handle Handle
     * @return Long
     */
    public Long getLong(String handle) {
        return get(handle).asLong();
    }

    /**
     * Get a Long by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Long
     */
    public Long getLong(String handle, Long def) {
        return get(handle, def).asLong();
    }

    /**
     * Get a Long List by Handle
     *
     * @param handle Handle
     * @return Long List
     */
    public List<Long> getLongList(String handle) {
        return get(handle).asLongList();
    }

    /**
     * Get a Long List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Long List
     */
    public List<Long> getLongList(String handle, List<Long> def) {
        return get(handle).asLongList();
    }

    /**
     * Get a Short by Handle
     *
     * @param handle Handle
     * @return Short
     */
    public Short getShort(String handle) {
        return get(handle).asShort();
    }

    /**
     * Get a Short by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Short
     */
    public Short getShort(String handle, Short def) {
        return get(handle, def).asShort();
    }

    /**
     * Get a Short List by Handle
     *
     * @param handle Handle
     * @return Short List
     */
    public List<Short> getShortList(String handle) {
        return get(handle).asShortList();
    }

    /**
     * Get a Short List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Short List
     */
    public List<Short> getShortList(String handle, List<Short> def) {
        return get(handle).asShortList();
    }

    /**
     * Get an Unparsed String by Handle
     *
     * @param handle Handle
     * @return Unparsed String
     */
    public String getRawString(String handle) {
        return get(handle).asRawString();
    }

    /**
     * Get an Unparsed String by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Unparsed String
     */
    public String getRawString(String handle, String def) {
        return get(handle, def).asRawString();
    }

    /**
     * Get an Unparsed String List by Handle
     *
     * @param handle Handle
     * @return Unparsed String List
     */
    public List<String> getRawStringList(String handle) {
        return get(handle).asRawStringList();
    }

    /**
     * Get an Unparsed String List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return Unparsed String List
     */
    public List<String> getRawStringList(String handle, List<String> def) {
        return get(handle, def).asRawStringList();
    }

    /**
     * Get a String by Handle
     *
     * @param handle Handle
     * @return String
     */
    public String getString(String handle) {
        return get(handle).asString();
    }

    /**
     * Get a String by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return String
     */
    public String getString(String handle, String def) {
        return get(handle, def).asString();
    }

    /**
     * Get a String List by Handle
     *
     * @param handle Handle
     * @return String List
     */
    public List<String> getStringList(String handle) {
        return get(handle).asStringList();
    }

    /**
     * Get a String List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return String List
     */
    public List<String> getStringList(String handle, List<String> def) {
        return get(handle, def).asStringList();
    }

    /**
     * Get a Colored String by Handle
     *
     * @param handle Handle
     * @param color Color Char to parse
     * @return Colored String
     */
    public String getColoredString(String handle, char color) {
        return get(handle).asColoredString(color);
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
        return get(handle, def).asColoredString(color);
    }
    /**
     * Get a Colored String List by Handle
     *
     * @param handle Handle
     * @param color Color Char to parse
     * @return Colored String List
     */
    public List<String> getColoredStringList(String handle, char color) {
        return get(handle).asColoredStringList(color);
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
        return get(handle, def).asColoredStringList(color);
    }

    /**
     * Get a UUID by Handle
     *
     * @param handle Handle
     * @return UUID
     */
    public UUID getUUID(String handle) {
        return get(handle).asUUID();
    }

    /**
     * Get a UUID by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return UUID
     */
    public UUID getUUID(String handle, UUID def) {
        return get(handle, def).asUUID();
    }

    /**
     * Get a UUID List by Handle
     *
     * @param handle Handle
     * @return UUID List
     */
    public List<UUID> getUUIDList(String handle) {
        return get(handle).asUUIDList();
    }

    /**
     * Get a UUID List by Handle
     *
     * @param handle Handle
     * @param def Default
     * @return UUID List
     */
    public List<UUID> getUUIDList(String handle, List<UUID> def) {
        return get(handle, def).asUUIDList();
    }

    /**
     * Check if object is Null by Handle
     *
     * @param handle Handle
     * @return Object Null Status
     */
    public boolean isNull(String handle) {
        return get(handle).isNull();
    }

    /**
     * Check if object is a Boolean by Handle
     *
     * @param handle Handle
     * @return Object Boolean Status
     */
    public boolean isBoolean(String handle) {
        return get(handle).isBoolean();
    }

    /**
     * Check if object is a YAML Section by Handle
     *
     * @param handle Handle
     * @return Object YAML Section Status
     */
    public boolean isSection(String handle) {
        return get(handle).isSection();
    }

    /**
     * Check if object is a List by Handle
     *
     * @param handle Handle
     * @return Object List Status
     */
    public boolean isList(String handle) {
        return get(handle).isList();
    }

    /**
     * Check if object is a Number by Handle
     *
     * @param handle Handle
     * @return Number Status
     */
    public boolean isNumber(String handle) {
        return get(handle).isNumber();
    }

    /**
     * Check if object is a String by Handle
     *
     * @param handle Handle
     * @return Object String Status
     */
    public boolean isString(String handle) {
        return get(handle).isString();
    }

    /**
     * Check if object is a UUID by Handle
     *
     * @param handle Handle
     * @return Object UUID Status
     */
    public boolean isUUID(String handle) {
        return get(handle).isUUID();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof YAMLSection) {
            return map.equals(((YAMLSection) object).map);
        } else {
            return super.equals(object);
        }
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
}
