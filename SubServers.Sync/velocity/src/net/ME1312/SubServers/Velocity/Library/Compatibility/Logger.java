package net.ME1312.SubServers.Velocity.Library.Compatibility;

import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Logger Compatibility Class
 */
public class Logger {
    private static final HashMap<String, java.util.logging.Logger> existing = new HashMap<String, java.util.logging.Logger>();
    private static org.apache.logging.log4j.Logger parent;

    /**
     * Get a logger
     *
     * @param prefix Prefix
     * @return Logger
     */
    public static java.util.logging.Logger get(String prefix) {
        if (!existing.keySet().contains(prefix)) {
            java.util.logging.Logger log = java.util.logging.Logger.getAnonymousLogger();
                log.setUseParentHandlers(false);
                log.addHandler(new Handler() {
                    @Override
                    public void publish(LogRecord record) {
                        String message = prefix + " > " + record.getMessage();
                        if (record.getLevel() == Level.INFO) {
                            parent.info(message, record.getParameters());
                        } else if (record.getLevel() == Level.WARNING) {
                            parent.warn(message, record.getParameters());
                        } else if (record.getLevel() == Level.SEVERE) {
                            parent.error(message, record.getParameters());
                        } else if (record.getLevel().intValue() < Level.FINE.intValue()) {
                            parent.trace(message, record.getParameters());
                        } else if (record.getLevel().intValue() < Level.INFO.intValue()) {
                            parent.debug(message, record.getParameters());
                        }
                    }

                    @Override
                    public void flush() {}
                    public void close() {}
                });
            existing.put(prefix, log);
        }
        return existing.get(prefix);
    }
}
