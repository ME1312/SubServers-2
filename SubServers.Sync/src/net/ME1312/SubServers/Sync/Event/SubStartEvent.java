package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Start Server Event
 */
public class SubStartEvent extends Event implements SubEvent {
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
        Util.nullpo(server);
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
