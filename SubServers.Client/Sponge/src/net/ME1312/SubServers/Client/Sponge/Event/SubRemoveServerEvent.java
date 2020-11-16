package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Remove Server Event
 */
public class SubRemoveServerEvent extends AbstractEvent implements SubEvent {
    private UUID player;
    private String host;
    private String server;

    /**
     * Server Remove Event
     *
     * @param player Player Adding Host
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

    /**
     * Gets the cause of this Event
     *
     * @deprecated Use simplified methods where available
     * @return The player UUID who triggered this event
     */
    @Override
    @Deprecated
    public Cause getCause() {
        return Cause.builder().append(player).build(getContext());
    }
}
