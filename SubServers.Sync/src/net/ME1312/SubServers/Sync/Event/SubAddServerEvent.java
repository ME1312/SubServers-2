package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Add Server Event
 */
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
        Util.nullpo(server);
        this.player = player;
        this.host = host;
        this.server = server;
    }

    /**
     * Gets the Server to be Added
     *
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
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }
}
