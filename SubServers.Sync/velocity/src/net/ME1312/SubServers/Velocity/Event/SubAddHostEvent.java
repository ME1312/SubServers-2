package net.ME1312.SubServers.Velocity.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Velocity.Library.SubEvent;

import java.util.UUID;

/**
 * Add Server Event
 */
public class SubAddHostEvent implements SubEvent {
    private UUID player;
    private String host;

    /**
     * Server Add Event
     *
     * @param player Player Adding Server
     * @param host Host to be added
     */
    public SubAddHostEvent(UUID player, String host) {
        Util.nullpo(host);
        this.player = player;
        this.host = host;
    }

    /**
     * Gets the Host to be Added
     *
     * @return The Host to be Added
     */
    public String getHost() { return host; }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }
}
