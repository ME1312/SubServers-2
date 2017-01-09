package net.ME1312.SubServers.Bungee.Host;

/**
 * SubLogger Layout Class
 */
public abstract class SubLogger {

    /**
     * Gets the Name of the task logging
     *
     * @return Log Task Name
     */
    public abstract String getName();

    /**
     * Start Logger
     */
    public abstract void start();

    /**
     * Stop Logger
     */
    public abstract void stop();

    public abstract boolean isLogging();

    /**
     * Register Filter
     *
     * @param filter Filter
     */
    public abstract void registerFilter(SubLogFilter filter);

    /**
     * Unregister Filter
     *
     * @param filter Filter
     */
    public abstract void unregisterFilter(SubLogFilter filter);
}
