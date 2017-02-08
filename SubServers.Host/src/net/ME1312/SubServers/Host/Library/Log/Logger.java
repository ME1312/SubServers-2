package net.ME1312.SubServers.Host.Library.Log;

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
     * @param dir Runtime Directory
     * @throws IOException
     */
    public static void setup(PrintStream out, PrintStream err, File dir) throws IOException {
        if (Util.isNull(out, err, dir)) throw new NullPointerException();
        if (pso.get() == null || pse.get() == null) {
            File runtime = new File(URLDecoder.decode(System.getProperty("subservers.host.runtime", "./"), "UTF-8"));
            pso.set(new PrintStream(new FileLogger(out, dir)));
            pse.set(new PrintStream(new FileLogger(err, dir)));
            System.setOut(new PrintStream(new SystemLogger(false, runtime)));
            System.setErr(new PrintStream(new SystemLogger(true, runtime)));
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
        warn = new LogStream(prefix, "WARN", pso);
        error = new ErrorStream(prefix, "ERROR", pse);
        severe = new ErrorStream(prefix, "SEVERE", pse);
    }

    public final LogStream message;
    public final LogStream info;
    public final LogStream warn;
    public final ErrorStream error;
    public final ErrorStream severe;
}
