package net.ME1312.SubServers.Bungee.Host;

/**
 * SubLogger Layout Class
 */
public abstract class SubLogger {
    public static final int MAX_GC = Integer.getInteger("subservers.logging.max_gc", 4096);
    private static boolean gc_running = false;
    protected static int gc = 0;
    protected static void gc() {
        if (!gc_running && MAX_GC > 0 && gc >= MAX_GC) {
            gc_running = true;
            System.gc();
            gc = 0;
            gc_running = false;
        }
    }

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
