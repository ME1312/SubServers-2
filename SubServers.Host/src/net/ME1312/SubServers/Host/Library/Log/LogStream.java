package net.ME1312.SubServers.Host.Library.Log;

import net.ME1312.SubServers.Host.Library.Container;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Log Stream Class
 */
public class LogStream {
    private static LogStream last = null;
    private String prefix;
    private String name;
    private Container<PrintStream> stream;
    private boolean first = true;

    protected LogStream(String prefix, String name, Container<PrintStream> stream) {
        this.prefix = prefix;
        this.name = name;
        this.stream = stream;
    }

    protected String prefix() {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] [" + prefix + File.separator + name + "] > ";
    }

    /**
     * Print an Object
     *
     * @param obj Object
     */
    public void print(Object obj) {
        print(obj.toString());
    }

    /**
     * Print a String
     *
     * @param str String
     */
    public void print(String str) {
        print(str.toCharArray());
    }

    /**
     * Print an Array of Characters
     *
     * @param str Character Array
     */
    public void print(char[] str) {
        for (char c : str) print(c);
    }

    /**
     * Print a Character
     *
     * @param c Character
     */
    public void print(char c) {
        if (last != this) {
            if (last != null && !last.first) last.print('\n');
            LogStream.last = this;
        }
        String str = "";
        if (first) str += prefix();
        str += c;
        first = c == '\n';
        stream.get().print(str);
    }

    /**
     * Print multiple Objects (separated by a new line)
     *
     * @param obj Objects
     */
    public void println(Object... obj) {
        for (Object o : obj) {
            print(o);
            print('\n');
        }
    }

    /**
     * Print multiple Strings (separated by a new line)
     *
     * @param str Objects
     */
    public void println(String... str) {
        for (String s : str) {
            print(s);
            print('\n');
        }
    }

    /**
     * Print multiple Arrays of Characters (separated by a new line)
     *
     * @param str Character Arrays
     */
    public void println(char[]... str) {
        for (char[] s : str) {
            print(s);
            print('\n');
        }
    }

    /**
     * Print multiple Characters (separated by a new line)
     *
     * @param c Characters
     */
    public void println(char... c) {
        for (char character : c) {
            print(character);
            print('\n');
        }
    }
}
