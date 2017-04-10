package net.ME1312.SubServers.Host.Library.Log;

import net.ME1312.SubServers.Host.Library.NamedContainer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * System.out and System.err Override Class
 */
public final class SystemLogger extends OutputStream {
    private NamedContainer<String, Logger> last = new NamedContainer<String, Logger>("", null);
    private boolean error;
    private File dir;

    protected SystemLogger(boolean level, File dir) throws IOException {
        if (!new File(dir, SystemLogger.class.getCanonicalName().replace(".", File.separator) + ".class").exists()) {
            throw new IOException("Invalid directory for logging:" + dir.getPath());
        }
        this.error = level;
        this.dir = dir;
    }

    @Override
    public void write(int c) throws IOException {
        int i = 0;
        String origin = java.lang.System.class.getCanonicalName();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (i > 1 && new File(dir, element.getClassName().replace(".", File.separator) + ".class").exists()) {
                origin = element.getClassName().replaceFirst("\\$.*", "");
                break;
            }
            i++;
        }
        if (!last.name().equals(origin)) last = new NamedContainer<String, Logger>(origin, new Logger(origin));
        if (error) {
            last.get().error.print((char) c);
        } else {
            last.get().info.print((char) c);
        }
    }
}

