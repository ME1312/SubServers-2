package net.ME1312.SubServers.Velocity.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Velocity.Library.SubEvent;

import java.util.UUID;

/**
 * Remove Host Event
 */
public class SubRemoveHostEvent implements SubEvent {
    private UUID player;
    private String host;

    /**
     * Host Remove Event
     *
     * @param player Player Adding Host
     * @param host Server Starting
     */
    public SubRemoveHostEvent(UUID player, String host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.player = player;
        this.host = host;
    }

    /**
     * Gets the Host to be Removed
     *
     * @return The Host to be Removed
     */
    public String getHost() { return host; }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }
}
