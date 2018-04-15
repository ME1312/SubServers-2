package net.ME1312.SubServers.Bungee.Library;

/**
 * Callback Class
 */
public interface Callback<T> {
    /**
     * Run the Callback
     *
     * @param obj Object
     */
    void run(T obj);
}
