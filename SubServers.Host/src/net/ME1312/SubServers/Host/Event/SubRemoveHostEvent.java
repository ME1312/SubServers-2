package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Event.Event;
import net.ME1312.Galaxi.Library.Util;

import java.util.UUID;

/**
 * Remove Host Event
 */
public class SubRemoveHostEvent extends Event {
    private UUID player;
    private String host;

    /**
     * Server Host Event
     *
     * @param player Player Adding Host
     * @param host Server Starting
     */
    public SubRemoveHostEvent(UUID player, String host) {
        Util.nullpo(host);
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
