package net.ME1312.SubServers.Host.Library.Log;

import net.ME1312.SubServers.Host.Library.Container;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error Log Stream Class
 */
public class ErrorStream extends LogStream {
    protected ErrorStream(String prefix, String name, Container<PrintStream> stream) {
        super(prefix, name, stream);
    }

    /**
     * Print an Exception
     *
     * @param err Exception
     */
    public void print(Throwable err) {
        StringWriter sw = new StringWriter();
        err.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        print(s.substring(0, s.length() - 1));
    }

    /**
     * Print multiple Exceptions (separated by a new line)
     *
     * @param err Exceptions
     */
    public void println(Throwable... err) {
        for (Throwable e : err) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            print(sw.toString());
        }
    }
}
