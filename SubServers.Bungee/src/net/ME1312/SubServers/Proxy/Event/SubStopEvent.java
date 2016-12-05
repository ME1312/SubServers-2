package net.ME1312.SubServers.Proxy.Event;

import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

public class SubStopEvent extends Event {
    private boolean cancelled = false;
    private UUID player;
    private SubServer server;
    private boolean force;

    /**
     * Server Stop Event
     *
     * @param server Server Stopping
     * @param player Player Stopping Server
     * @param force If it was a Forced Shutdown
     */
    public SubStopEvent(SubServer server, UUID player, boolean force) {
        this.player = player;
        this.server = server;
        this.force = force;
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
     * Gets if it was a forced shutdown
     *
     * @return Forced Shutdown Status
     */
    public boolean isForced() {
        return force;
    }

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
