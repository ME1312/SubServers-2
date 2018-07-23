package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Server Shell Exit Event
 */
public class SubStoppedEvent extends AbstractEvent implements SubEvent {
    private String server;

    /**
     * Server Shell Exit Event
     *
     * @param server Server that Stopped
     */
    public SubStoppedEvent(String server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public String getServer() { return server; }

    /**
     * Gets the cause of this Event
     *
     * @return An empty cause list
     */
    @Override
    public Cause getCause() {
        return Cause.builder().build();
    }
}
