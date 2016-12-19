package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class SubAddServerEvent extends Event implements SubEvent {
    private UUID player;
    private String host;
    private String server;

    /**
     * Server Add Event
     *
     * @param player Player Adding Server
     * @param server Server Starting
     */
    public SubAddServerEvent(UUID player, String host, String server) {
        this.player = player;
        this.host = host;
        this.server = server;
    }

    /**
     * Gets the Server to be Added
     * @return The Server to be Added
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
