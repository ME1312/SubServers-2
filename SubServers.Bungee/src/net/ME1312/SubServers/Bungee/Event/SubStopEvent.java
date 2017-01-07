package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Stop Event
 */
public class SubStopEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private SubServer server;
    private boolean force;

    /**
     * Server Stop Event
     *
     * @param player Player Stopping Server
     * @param server Server Stopping
     * @param force If it was a Forced Shutdown
     */
    public SubStopEvent(UUID player, SubServer server, boolean force) {
        this.player = player;
        this.server = server;
        this.force = force;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
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
     *
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
