package net.ME1312.SubServers.Bungee.Library;

/**
 * Container Class
 *
 * @author ME1312
 */
public class Container<V> {
    private V obj;

    /**
     * Creates a Container
     *
     * @param item Object to Store
     */
    public Container(V item) {
        obj = item;
    }

    /**
     * Grabs the Object
     *
     * @return The Object
     */
    public V get() {
        return obj;
    }

    /**
     * Overwrite the Object
     *
     * @param value Object to Store
     */
    public void set(V value) {
        obj = value;
    }
}
