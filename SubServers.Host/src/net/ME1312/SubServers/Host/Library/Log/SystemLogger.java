package net.ME1312.SubServers.Host.Library.Log;

import net.ME1312.SubServers.Host.Library.NamedContainer;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

/**
 * System.out and System.err Override Class
 */
public final class SystemLogger extends OutputStream {
    private NamedContainer<String, Logger> last = new NamedContainer<String, Logger>("", null);
    private boolean error;

    protected SystemLogger(boolean level) throws IOException {
        this.error = level;
    }

    @SuppressWarnings("unchecked")
    private List<String> getKnownClasses() {
        List<String> value = null;
        try {
            Field f = SubAPI.class.getDeclaredField("knownClasses");
            f.setAccessible(true);
            value = (List<String>) f.get(SubAPI.getInstance());
            f.setAccessible(false);
        } catch (Exception e) {}
        return value;
    }

    @Override
    public void write(int c) throws IOException {
        int i = 0;
        String origin = java.lang.System.class.getCanonicalName();
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (i > 1 && getKnownClasses().contains(element.getClassName())) {
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

