package net.ME1312.SubServers.Client.Bukkit.Library;

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
