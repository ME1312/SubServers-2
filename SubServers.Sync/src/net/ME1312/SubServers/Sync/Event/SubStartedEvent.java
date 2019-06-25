package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Sync.Library.SubEvent;
import net.md_5.bungee.api.plugin.Event;

/**
 * Server Started Event
 */
public class SubStartedEvent extends Event implements SubEvent {
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
