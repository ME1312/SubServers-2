package net.ME1312.SubServers.Sync.Library;

/**
 * Named Container Class
 * @param <T> Name
 * @param <V> Item
 */
public class NamedContainer<T, V> extends Container<V> {
    private T name;

    /**
     * Creates a TaggedContainer
     *
     * @param name Tag to Bind
     * @param item Object to Store
     */
    public NamedContainer(T name, V item) {
        super(item);
        this.name = name;
    }

    /**
     * Gets the name of the Container
     *
     * @return Container name
     */
    public T name() {
        return name;
    }

    /**
     * Renames the Container
     *
     * @param name New Container Name
     */
    public void rename(T name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NamedContainer) {
            if (name == null || ((NamedContainer) object).name() == null) {
                return name == ((NamedContainer) object).name() && super.equals(object);
            } else {
                return name.equals(((NamedContainer) object).name()) && super.equals(object);
            }
        } else {
            return super.equals(object);
        }
    }
}
