package net.ME1312.SubServers.Bungee.Host;

import java.util.logging.Level;

/**
 * SubServer Log Filter Layout Class
 */
public interface SubLogFilter {

    /**
     * Called when the logging has started
     */
    void start();

    /**
     * Determine if this message should be logged
     *
     * @param level Log Level
     * @param message Message to Log
     * @return If this message should be logged
     */
    boolean log(Level level, String message);

    /**
     * Called when the logging has stopped
     */
    void stop();
}
