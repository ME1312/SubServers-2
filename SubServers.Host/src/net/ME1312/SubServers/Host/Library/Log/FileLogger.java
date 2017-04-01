package net.ME1312.SubServers.Host.Library.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Log File Writer Class
 */
public final class FileLogger extends OutputStream {
    private static FileWriter file = null;
    private PrintStream origin;

    protected FileLogger(PrintStream origin, File dir) throws IOException {
        this.origin = origin;
        if (file == null) {
            new File(dir, "Logs").mkdirs();
            file = new FileWriter(new File(dir, "Logs" + File.separator + "SubServers #" + (new File(dir, "Logs").list().length + 1) + " (" + new SimpleDateFormat("MM-dd-yyyy").format(Calendar.getInstance().getTime()) + ").log"));
        }
    }

    @Override
    public void write(int b) throws IOException {
        origin.write((char)b);
        if (file != null) {
            file.write((char) b);
            file.flush();
        }
    }

    public static void end() throws IOException {
        if (file != null) file.close();
        file = null;
    }
}
