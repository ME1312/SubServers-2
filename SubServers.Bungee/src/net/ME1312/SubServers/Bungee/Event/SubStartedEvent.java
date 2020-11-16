package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * Server Started Event
 */
public class SubStartedEvent extends Event implements SubEvent {
    private SubServer server;

    /**
     * Server Started Event<br>
     * <b>This event can only be called when a SubData connection is made!</b>
     *
     * @param server Server Starting
     */
    public SubStartedEvent(SubServer server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }
}
