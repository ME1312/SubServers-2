package net.ME1312.SubServers.Host.Library.Event;

/**
 * Cancellable SubEvent Layout Class
 */
public interface Cancellable {
    /**
     * Gets if the Event has been Cancelled
     *
     * @return Cancelled Status
     */
    boolean isCancelled();

    /**
     * Sets if the Event is Cancelled
     *
     * @param value
     */
    void setCancelled(boolean value);
}
