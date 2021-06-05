package net.ME1312.SubServers.Bungee.Library;

import net.ME1312.Galaxi.Library.Util;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * File Replacement Scanner
 */
public class ReplacementScanner extends FileScanner {
    private final Map<String, String> replacements = new LinkedHashMap<>();

    public ReplacementScanner(Map<String, String> replacements) {
        TreeMap<Integer, LinkedList<String>> order = new TreeMap<Integer, LinkedList<String>>(Comparator.reverseOrder());
        for (String key : replacements.keySet()) {
            int length = key.length();
            if (!order.keySet().contains(length)) order.put(length, new LinkedList<>());
            order.get(length).add(key);
        }

        for (Integer length : order.keySet()) {
            for (String key : order.get(length)) {
                this.replacements.put(key, replacements.get(key));
            }
        }
    }

    /**
     * Get the replacements
     *
     * @return Replacement Map
     */
    public Map<String, String> getReplacements() {
        return new HashMap<>(replacements);
    }

    /**
     * Make replacements in a File or Directory
     *
     * @param dir File or Directory
     * @param whitelist File Whitelist
     */
    public void replace(File dir, String... whitelist) throws IOException {
        super.scan(dir, whitelist);
    }

    protected void act(File dir, String name) throws IOException {
        File file = new File(dir, name);
        FileInputStream stream = new FileInputStream(file);
        String string = Util.readAll(new InputStreamReader(stream));
        stream.close();

        boolean update = false;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            String placeholder = "SubServers::" + replacement.getKey();
            if (string.contains(placeholder)) {
                string = string.replace(placeholder, replacement.getValue());
                update = true;
            }
        }

        if (update) {
            FileWriter writer = new FileWriter(file, false);
            writer.write(string);
            writer.close();
        }
    }


    /**
     * Make replacements in an Object
     *
     * @param value Map, Collection, Array, or String
     * @return Object with replaced variables
     */
    public Object replace(Object value) {
        if (value instanceof Map) {
            List<String> list = new ArrayList<String>();
            list.addAll(((Map<String, Object>) value).keySet());
            for (String key : list) ((Map<String, Object>) value).put(key, replace(((Map<String, Object>) value).get(key)));
            return value;
        } else if (value instanceof Collection) {
            List<Object> list = new ArrayList<Object>();
            for (Object val : (Collection<Object>) value) list.add(replace(val));
            return list;
        } else if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < ((Object[]) value).length; i++) list.add(replace(((Object[]) value)[i]));
            return list;
        } else if (value instanceof String) {
            return replaceObj((String) value);
        } else {
            return value;
        }
    } private String replaceObj(String string) {
        for (Map.Entry<String, String> replacement : replacements.entrySet()) string = string.replace('$' + replacement.getKey() + '$', replacement.getValue());
        return string;
    }
}
