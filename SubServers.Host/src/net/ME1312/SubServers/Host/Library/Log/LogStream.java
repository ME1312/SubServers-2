package net.ME1312.SubServers.Host.Library.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * System.out and System.err Override Class
 */
public class LogStream extends OutputStream {
    private PrintStream origin;
    private File dir;
    private boolean type;
    private boolean first;

    protected LogStream(PrintStream origin, File dir, boolean type) throws IOException {
        if (!new File(dir, LogStream.class.getCanonicalName().replace(".", File.separator) + ".class").exists()) {
            throw new IOException("Invalid directory for logging:" + dir.getPath());
        }
        this.origin = origin;
        this.dir = dir;
        this.type = type;
        this.first = true;
    }

    @Override
    public void write(int b) throws IOException {
        String origin = null;
        for (StackTraceElement element : Arrays.asList(new NullPointerException().getStackTrace())) {
            try {
                Class e = Class.forName(element.getClassName());
                if (origin != null || e.getCanonicalName().equals(LogStream.class.getCanonicalName()) || e.getCanonicalName().equals(Logger.class.getCanonicalName()) || !new File(this.dir, e.getCanonicalName().replace(".", File.separator) + ".class").exists()) continue;
                origin = e.getCanonicalName();
            }
            catch (ClassNotFoundException e) {}
        }
        if (origin == null) {
            origin = "System";
        }
        String value = Character.toString((char)b);
        StringBuilder s = new StringBuilder();
        Logger log = new Logger(origin);
        if (this.first) {
            s.append(log.prefix(this.type ? "INFO" : "ERROR"));
        }
        s.append(value);
        this.first = value.equals("\n");
        this.origin.print(s.toString());
    }
}
