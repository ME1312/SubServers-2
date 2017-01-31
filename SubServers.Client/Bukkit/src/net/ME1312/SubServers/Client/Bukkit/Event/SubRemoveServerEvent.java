package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Remove Server Event
 */
public class SubRemoveServerEvent extends Event implements SubEvent {
    private UUID player;
    private String host;
    private String server;

    /**
     * Server Remove Event
     *
     * @param player Player Adding Server
     * @param server Server Starting
     */
    public SubRemoveServerEvent(UUID player, String host, String server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.player = player;
        this.host = host;
        this.server = server;
    }

    /**
     * Gets the Server to be Removed
     *
     * @return The Server to be Removed
     */
    public String getServer() { return server; }

    /**
     * Gets the Host of the Server
     *
     * @return The Host of the Server or null if isn't a SubServer
     */
    public String getHost() {
        return host;
    }

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
