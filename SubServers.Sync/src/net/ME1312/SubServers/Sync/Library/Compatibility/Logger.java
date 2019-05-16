package net.ME1312.SubServers.Sync.Library.Compatibility;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Sync.SubAPI;
import net.ME1312.SubServers.Sync.SubPlugin;
import net.md_5.bungee.api.ProxyServer;

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Logger Compatibility Class
 */
public class Logger {
    private static final HashMap<String, java.util.logging.Logger> existing = new HashMap<String, java.util.logging.Logger>();
    private static SubPlugin plugin;

    /**
     * Get a logger
     *
     * @param prefix Prefix
     * @return Logger
     */
    @SuppressWarnings("deprecation")
    public static java.util.logging.Logger get(String prefix) {
        if (!existing.keySet().contains(prefix)) {
            SubPlugin plugin = SubAPI.getInstance().getInternals();
            java.util.logging.Logger log;

            if (plugin.isGalaxi) {
                log = Util.getDespiteException(() -> Util.reflect(Class.forName("net.ME1312.Galaxi.Library.Log.Logger").getDeclaredMethod("toPrimitive"),
                        Util.reflect(Class.forName("net.ME1312.Galaxi.Library.Log.Logger").getConstructor(String.class), prefix)), null);
            } else {
                log = java.util.logging.Logger.getAnonymousLogger();
                log.setUseParentHandlers(false);
                log.addHandler(new Handler() {
                    private boolean open = true;

                    @Override
                    public void publish(LogRecord record) {
                        if (open)
                            plugin.getLogger().log(record.getLevel(), prefix + " > " + record.getMessage(), record.getParameters());
                    }

                    @Override
                    public void flush() {

                    }

                    @Override
                    public void close() throws SecurityException {
                        open = false;
                    }
                });
            }
            existing.put(prefix, log);
        }
        return existing.get(prefix);
    }
}
