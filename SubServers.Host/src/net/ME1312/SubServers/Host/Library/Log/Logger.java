package net.ME1312.SubServers.Host.Library.Log;

import jline.console.ConsoleReader;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;

/**
 * Logger Class
 */
public final class Logger {
    private static final Container<PrintStream> pso = new Container<PrintStream>(null);
    private static final Container<PrintStream> pse = new Container<PrintStream>(null);

    /**
     * Setup the SubServers Log System
     *
     * @param out System.out
     * @param err System.err
     * @param in jline.in
     * @param dir Runtime Directory
     * @throws IOException
     */
    public static void setup(PrintStream out, PrintStream err, ConsoleReader in, File dir) throws IOException {
        if (Util.isNull(out, err, dir)) throw new NullPointerException();
        if (pso.get() == null || pse.get() == null) {
            pso.set(new PrintStream(new FileLogger(new ConsoleStream(in, out), dir)));
            pse.set(new PrintStream(new FileLogger(new ConsoleStream(in, err), dir)));
            System.setOut(new PrintStream(new SystemLogger(false)));
            System.setErr(new PrintStream(new SystemLogger(true)));
        }
    }

    /**
     * Gets a new Logger
     *
     * @param prefix Log Prefix
     */
    public Logger(String prefix) {
        if (Util.isNull(prefix)) throw new NullPointerException();
        if (prefix.length() == 0) throw new StringIndexOutOfBoundsException("Cannot use an empty prefix");
        message = new LogStream(prefix, "MESSAGE", pso);
        info = new LogStream(prefix, "INFO", pso);
        warn = new ErrorStream(prefix, "WARN", pso);
        error = new ErrorStream(prefix, "ERROR", pse);
        severe = new ErrorStream(prefix, "SEVERE", pse);
    }

    public final LogStream message;
    public final LogStream info;
    public final ErrorStream warn;
    public final ErrorStream error;
    public final ErrorStream severe;
}
