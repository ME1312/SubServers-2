package net.ME1312.SubServers.Client.Sponge.Library;

import java.io.*;
import java.util.*;

/**
 * SubServers Utility Class
 */
public final class Util {
    private Util(){}
    public interface ExceptionReturnRunnable<R> {
        R run() throws Throwable;
    }
    public interface ExceptionRunnable {
        void run() throws Throwable;
    }
    public interface ReturnRunnable<R> {
        R run();
    }

    /**
     * Checks values to make sure they're not null
     *
     * @param values Values to check
     * @return If any are null
     */
    public static boolean isNull(Object... values) {
        boolean ret = false;
        for (Object value : values) {
            if (value == null) ret = true;
        }
        return ret;
    }

    /**
     * Get keys by value from map
     *
     * @param map Map to search
     * @param value Value to search for
     * @param <K> Key
     * @param <V> Value
     * @return Search results
     */
    public static <K, V> List<K> getBackwards(Map<K, V> map, V value) {
        List<K> values = new ArrayList<K>();

        for (K key : map.keySet()) {
            if (map.get(key).equals(value)) {
                values.add(key);
            }
        }

        return values;
    }

    /**
     * Get an item from a map ignoring case
     *
     * @param map Map to search
     * @param key Key to search with
     * @param <V> Value
     * @return Search Result
     */
    public static <V> V getCaseInsensitively(Map<String, V> map, String key) {
        HashMap<String, String> insensitivity = new HashMap<String, String>();
        for (String item : map.keySet()) insensitivity.put(item.toLowerCase(), item);
        if (insensitivity.keySet().contains(key.toLowerCase())) {
            return map.get(insensitivity.get(key.toLowerCase()));
        } else {
            return null;
        }
    }

    /**
     * Gets a new Variable that doesn't match the existing Variables
     *
     * @param existing Existing Variables
     * @param generator Variable Generator
     * @param <V> Variable Type
     * @return Variable
     */
    public static <V> V getNew(Collection<? extends V> existing, ReturnRunnable<V> generator) {
        V result = null;
        while (result == null) {
            V tmp = generator.run();
            if (!existing.contains(tmp)) result = tmp;
        }
        return result;
    }

    /**
     * Read Everything from Reader
     *
     * @param rd Reader
     * @return Reader Contents
     * @throws IOException
     */
    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Copy from the Class Loader
     *
     * @param loader ClassLoader
     * @param resource Location From
     * @param destination Location To
     */
    public static void copyFromJar(ClassLoader loader, String resource, String destination) {
        InputStream resStreamIn = loader.getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get a variable from a method which may throw an exception
     *
     * @param runnable Runnable
     * @param def Default value when an exception is thrown
     * @param <R> Variable Type
     * @return Returns value or default depending on if an exception is thrown
     */
    public static <R> R getDespiteException(ExceptionReturnRunnable<R> runnable, R def) {
        try {
            return runnable.run();
        } catch (Throwable e) {
            return def;
        }
    }

    /**
     * Determines if an Exception will occur
     *
     * @param runnable Runnable
     * @return If an Exception occured
     */
    public static boolean isException(ExceptionRunnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (Throwable e) {
            return true;
        }
    }

    /**
     * Delete a Directory
     *
     * @param folder Location
     */
    public static void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * Copy a Directory
     *
     * @param from Source
     * @param to Destination
     */
    public static void copyDirectory(File from, File to) {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdirs();
            }

            String files[] = from.list();

            for (String file : files) {
                File srcFile = new File(from, file);
                File destFile = new File(to, file);

                copyDirectory(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(to);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            } catch (Exception e) {
                try {
                    if (in != null) in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    if (out != null) out.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * Get a Random Integer
     *
     * @param min Minimum Value
     * @param max Maximum Value
     * @return Random Integer
     */
    public static int random(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    /**
     * Parse escapes in a Java String
     *
     * @param str String
     * @return Unescaped String
     */
    public static String unescapeJavaString(String str) {
        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            int ch = str.codePointAt(i);
            if (ch == '\\') {
                int nextChar = (i == str.length() - 1) ? '\\' : str
                        .codePointAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    StringBuilder code = new StringBuilder();
                    code.appendCodePoint(nextChar);
                    i++;
                    if ((i < str.length() - 1) && str.codePointAt(i + 1) >= '0'
                            && str.codePointAt(i + 1) <= '7') {
                        code.appendCodePoint(str.codePointAt(i + 1));
                        i++;
                        if ((i < str.length() - 1) && str.codePointAt(i + 1) >= '0'
                                && str.codePointAt(i + 1) <= '7') {
                            code.appendCodePoint(str.codePointAt(i + 1));
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code.toString(), 8));
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
                    // Hex Unicode Char: u????
                    // Hex Unicode Codepoint: u{??????}
                    case 'u':
                        try {
                            if (i >= str.length() - 4) throw new IllegalStateException();
                            StringBuilder escape = new StringBuilder();
                            int offset = 2;

                            if (str.codePointAt(i + 2) != '{') {
                                if (i >= str.length() - 5) throw new IllegalStateException();
                                while (offset <= 5) {
                                    Integer.toString(str.codePointAt(i + offset), 16);
                                    escape.appendCodePoint(str.codePointAt(i + offset));
                                    offset++;
                                }
                                offset--;
                            } else {
                                offset++;
                                while (str.codePointAt(i + offset) != '}') {
                                    Integer.toString(str.codePointAt(i + offset), 16);
                                    escape.appendCodePoint(str.codePointAt(i + offset));
                                    offset++;
                                }
                            }
                            sb.append(new String(new int[]{
                                    Integer.parseInt(escape.toString(), 16)
                            }, 0, 1));

                            i += offset;
                            continue;
                        } catch (Throwable e){
                            sb.append('\\');
                            ch = 'u';
                            break;
                        }
                }
                i++;
            }
            sb.appendCodePoint(ch);
        }
        return sb.toString();
    }
}
