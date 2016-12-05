package net.ME1312.SubServers.Proxy.Libraries;

/**
 * Container Class
 *
 * @author ME1312
 */
public class Container<T> {
    private T obj;

    /**
     * Creates a Container
     *
     * @param item Object to Store
     */
    public Container(T item) {
        obj = item;
    }

    /**
     * Grabs the Object
     *
     * @return The Object
     */
    public T get() {
        return obj;
    }

    /**
     * Overwrite the Object
     *
     * @param value Object to Store
     */
    public void set(T value) {
        obj = value;
    }
}
