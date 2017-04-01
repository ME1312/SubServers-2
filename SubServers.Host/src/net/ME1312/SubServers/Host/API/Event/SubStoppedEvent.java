package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;

/**
 * Server Shell Exit Event
 */
public class SubStoppedEvent extends Event {
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
    public String getServer() {
        return server;
    }
}
