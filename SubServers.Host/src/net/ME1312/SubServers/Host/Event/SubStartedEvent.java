package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Event.Event;
import net.ME1312.Galaxi.Library.Util;

/**
 * Server Started Event
 */
public class SubStartedEvent extends Event {
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
    public String getServer() {
        return server;
    }
}
