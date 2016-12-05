package net.ME1312.SubServers.Proxy.Event;

import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.md_5.bungee.api.plugin.Event;

public class SubStoppedEvent extends Event {
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
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }
}
