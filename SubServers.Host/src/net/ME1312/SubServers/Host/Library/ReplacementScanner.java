package net.ME1312.SubServers.Host.Library;

import net.ME1312.Galaxi.Library.Util;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * File Replacement Scanner
 */
public class ReplacementScanner {
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
        List<String> files;
        try {
            files = Util.reflect(Util.class.getDeclaredMethod("zipsearch", File.class, File.class), null, dir, dir);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot access zipsearch()", e);
        }

        LinkedHashMap<Pattern, Boolean> rules = new LinkedHashMap<Pattern, Boolean>();
        for (String entry : whitelist) {
            boolean mode = !entry.startsWith("!");
            if (!mode) entry = entry.substring(1);
            if (entry.startsWith(".")) entry = entry.substring(1);

            StringBuilder rule = new StringBuilder();
            if (entry.startsWith("**")) {
                entry = entry.substring(2);
                rule.append("^.*");
            } else if (entry.startsWith("/")) {
                rule.append("^");
            }

            boolean greedyEnding = false;
            if (entry.endsWith("**")) {
                entry = entry.substring(0, entry.length() - 2);
                greedyEnding = true;
            } else if (entry.endsWith("/")) {
                greedyEnding = true;
            }

            StringBuilder literal = new StringBuilder();
            for (PrimitiveIterator.OfInt i = entry.codePoints().iterator(); i.hasNext(); ) {
                int c = i.next();
                if ((c == '*' || c == '?' || c == '[') && literal.length() > 0) {
                    rule.append(Pattern.quote(literal.toString()));
                    literal = new StringBuilder();
                }
                switch (c) {
                    case '\\':
                        if (i.hasNext()) c = i.next();
                        literal.appendCodePoint(c);
                    case '[':
                        for (boolean escaped = false; i.hasNext() && (c != ']' || escaped); c = i.next()) {
                            if (c == '\\') escaped = !escaped;
                            else escaped = false;
                            literal.appendCodePoint(c);
                        }
                        if (c == ']' && literal.length() > 1) {
                            literal.appendCodePoint(c);
                            rule.append(literal.toString());
                        }
                        literal = new StringBuilder();
                        break;
                    case '*':
                        rule.append("[^/]+");
                        break;
                    case '?':
                        rule.append("[^/]");
                        break;
                    default:
                        literal.appendCodePoint(c);
                        break;
                }
            }
            if (literal.length() > 0)
                rule.append(Pattern.quote(literal.toString()));

            if (greedyEnding)
                rule.append(".*");
            rule.append("$");
            rules.put(Pattern.compile(rule.toString()), mode);
        }

        for (String file : files) {
            boolean act = false;

            for (Map.Entry<Pattern, Boolean> rule : rules.entrySet()) {
                if (rule.getKey().matcher('/' + file.replace(File.separatorChar, '/')).find()) act = rule.getValue();
            }

            if (act) replaceFile(new File(dir, file));
        }
    } private void replaceFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        String string = Util.readAll(new InputStreamReader(stream));
        stream.close();

        for (Map.Entry<String, String> replacement : replacements.entrySet()) string = string.replace("SubServers::" + replacement.getKey(), replacement.getValue());
        FileWriter writer = new FileWriter(file, false);
        writer.write(string);
        writer.close();
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
