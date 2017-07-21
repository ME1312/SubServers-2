package net.ME1312.SubServers.Bungee.Library;

/**
 * Container Class
 *
 * @param <V> Item
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

    @Override
    public boolean equals(Object object) {
        if (object instanceof Container) {
            if (obj == null || ((Container) object).get() == null) {
                return obj == ((Container) object).get();
            } else {
                return obj.equals(((Container) object).get());
            }
        } else {
            return super.equals(object);
        }
    }
}
