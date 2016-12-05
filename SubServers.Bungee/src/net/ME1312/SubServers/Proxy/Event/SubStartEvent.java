package net.ME1312.SubServers.Proxy.Event;

import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

public class SubStartEvent extends Event {
    private boolean cancelled = false;
    private UUID player;
    private SubServer server;

    /**
     * Server Start Event
     *
     * @param server Server Starting
     * @param player Player Starting Server
     */
    public SubStartEvent(SubServer server, UUID player) {
        this.player = player;
        this.server = server;
    }

    /**
     * Gets the Server Effected
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }

    /**
     * Gets the player that Triggered the Event
     * @return The Player that triggered this Event or Null if Console
     */
    public UUID getPlayer() { return player; }

    /**
     * Gets the Cancelled Status
     * @return Cancelled Status
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the Cancelled Status
     */
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
