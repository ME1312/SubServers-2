package net.ME1312.SubServers.Client.Bukkit.Library;

/**
 * Callback Class
 */
public interface Callback<V> {
    /**
     * Run the Callback
     *
     * @param value Value
     */
    void run(V value);
}
