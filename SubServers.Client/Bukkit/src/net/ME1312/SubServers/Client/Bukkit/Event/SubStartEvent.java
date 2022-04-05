package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
        super(true);
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}
