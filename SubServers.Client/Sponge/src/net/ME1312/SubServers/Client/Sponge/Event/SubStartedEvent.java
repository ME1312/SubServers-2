package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Server Started Event
 */
public class SubStartedEvent extends AbstractEvent implements SubEvent {
    private String server;

    /**
     * Server Started Event<br>
     * <b>This event can only be called when a SubData connection is made!</b>
     *
     * @param server Server that Started
     */
    public SubStartedEvent(String server) {
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
     * @deprecated Use simplified methods where available
     * @return An empty cause list
     */
    @Override
    @Deprecated
    public Cause getCause() {
        return Cause.builder().build(getContext());
    }
}
