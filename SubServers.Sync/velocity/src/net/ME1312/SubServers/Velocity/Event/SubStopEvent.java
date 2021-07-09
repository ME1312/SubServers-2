package net.ME1312.SubServers.Velocity.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Velocity.Library.SubEvent;

import java.util.UUID;

/**
 * Server Stop Event
 */
public class SubStopEvent implements SubEvent {
    private UUID player;
    private String server;
    private boolean force;

    /**
     * Server Stop Event
     *
     * @param player Player Stopping Server
     * @param server Server Stopping
     * @param force  If it was a Forced Shutdown
     */
    public SubStopEvent(UUID player, String server, boolean force) {
        if (Util.isNull(server, force)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.force = force;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public String getServer() {
        return server;
    }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Gets if it was a forced shutdown
     *
     * @return Forced Shutdown Status
     */
    public boolean isForced() {
        return force;
    }
}
