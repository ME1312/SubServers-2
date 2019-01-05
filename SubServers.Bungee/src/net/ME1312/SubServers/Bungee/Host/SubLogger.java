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
     * Gets the Object using this Logger
     *
     * @return Object
     */
    public abstract Object getHandler();

    /**
     * Start Logger
     */
    public abstract void start();

    /**
     * Stop Logger
     */
    public abstract void stop();

    /**
     * Get if the Logger is currently logging
     *
     * @return Logging Status
     */
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
