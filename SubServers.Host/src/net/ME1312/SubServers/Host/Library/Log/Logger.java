package net.ME1312.SubServers.Host.Library.Log;

import net.ME1312.SubServers.Host.Library.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Logger Class
 */
public class Logger {
    private String s;
    private static boolean b = false;
    private static PrintStream out;
    private static PrintStream err;
    public enum Level {
        INFO(false, "INFO"),
        WARN(false, "WARN"),
        ERROR(true, "ERROR");

        private boolean err;
        private String name;
        Level(boolean err, String name) {
            this.err = err;
            this.name = name;
        }
        public PrintStream getStream() {
            return (err)?Logger.err:Logger.out;
        }
        public String getName() {
            return name;
        }
    }

    /**
     * Gets a new Logger
     *
     * @param prefix
     */
    public Logger(String prefix) {
        if (Util.isNull(prefix)) throw new NullPointerException();
        this.s = prefix;
    }

    /**
     * Setup the SubServers Logger
     *
     * @param out System.out
     * @param err System.err
     * @param dir Runtime Directory
     * @throws IOException
     */
    public static void setup(PrintStream out, PrintStream err, File dir) throws IOException {
        if (Util.isNull(out, err, dir)) throw new NullPointerException();
        if (!b) {
            File runtime = new File(URLDecoder.decode(System.getProperty("subservers.host.runtime", "./"), "UTF-8"));
            Logger.out = new PrintStream(new FileLogger(out, dir));
            Logger.err = Logger.out;
            System.setOut(new PrintStream(new LogStream(Logger.out, runtime, true)));
            System.setErr(new PrintStream(new LogStream(Logger.err, runtime, false)));
            b = true;
        }
    }

    protected String prefix(String status) {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] [" + this.s + "/" + status + "] > ";
    }

    /**
     * Log a message on the specified Level
     *
     * @param level Level to log to
     * @param messages Messages
     */
    public void log(Level level, String... messages) {
        if (Util.isNull(level)) throw new NullPointerException();
        Iterator<String> msgs = Arrays.asList(messages).iterator();
        while (msgs.hasNext()) {
            Iterator<String> items = Arrays.asList(msgs.next().split("\n")).iterator();
            while (items.hasNext()) {
                String str = this.prefix(level.getName()) + items.next();
                level.getStream().println(str);
            }
        }
    }

    /**
     * Log a message on the INFO Level
     *
     * @param messages Messages
     */
    public void info(String... messages) {
        log(Level.INFO, messages);
    }

    /**
     * Log a message on the WARNING Level
     *
     * @param messages Messages
     */
    public void warn(String ... messages) {
        log(Level.WARN, messages);
    }

    /**
     * Log a message on the ERROR Level
     *
     * @param messages Messages
     */
    public void error(String ... messages) {
        log(Level.ERROR, messages);
    }

    /**
     * Log an exception on the ERROR Level
     *
     * @param exception Exception
     */
    public void error(Throwable exception) {
        Throwable error = exception;
        String indent = "    ";
        boolean hasException = true;
        while (hasException) {
            String[] arrstring = new String[1];
            arrstring[0] = (!indent.substring(4).equals("") ? new StringBuilder().append(indent.substring(4)).append("Caused by ").toString() : "") + error.getClass().getCanonicalName() + ": " + error.getMessage();
            this.error(arrstring);
            Iterator<StackTraceElement> items = Arrays.asList(error.getStackTrace()).iterator();
            while (items.hasNext()) {
                this.error(indent + "at " + items.next().toString());
            }
            if (error instanceof InvocationTargetException) {
                this.error(indent);
                error = ((InvocationTargetException)error).getTargetException();
                indent = indent + "    ";
                continue;
            }
            hasException = false;
        }
    }
}
