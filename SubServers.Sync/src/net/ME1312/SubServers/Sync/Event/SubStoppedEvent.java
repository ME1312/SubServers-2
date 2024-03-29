package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * Server Shell Exit Event
 */
public class SubStoppedEvent extends Event implements SubEvent {
    private String server;

    /**
     * Server Shell Exit Event
     *
     * @param server Server that Stopped
     */
    public SubStoppedEvent(String server) {
        Util.nullpo(server);
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public String getServer() { return server; }
}
