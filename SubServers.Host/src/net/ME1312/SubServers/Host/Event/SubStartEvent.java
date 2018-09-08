package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Library.Event.Event;
import net.ME1312.Galaxi.Library.Util;

import java.util.UUID;

/**
 * Start Server Event
 */
public class SubStartEvent extends Event {
    private boolean cancelled = false;
    private UUID player;
    private String server;

    /**
     * Server Start Event
     *
     * @param player Player Starting Server
     * @param server Server Starting
     */
    public SubStartEvent(UUID player, String server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.player = player;
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public String getServer() { return server; }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }
}
