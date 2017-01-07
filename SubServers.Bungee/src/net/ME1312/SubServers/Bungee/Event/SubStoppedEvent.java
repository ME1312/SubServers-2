package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.md_5.bungee.api.plugin.Event;

/**
 * Server Shell Exit Event
 */
public class SubStoppedEvent extends Event implements SubEvent {
    private SubServer server;

    /**
     * Server Shell Exit Event
     *
     * @param server Server that Stopped
     */
    public SubStoppedEvent(SubServer server) {
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }
}
