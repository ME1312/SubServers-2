package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Add Server Event
 */
public class SubAddHostEvent extends AbstractEvent implements SubEvent {
    private UUID player;
    private String host;

    /**
     * Server Add Event
     *
     * @param player Player Adding Server
     * @param host Host to be added
     */
    public SubAddHostEvent(UUID player, String host) {
        if (Util.isNull(host)) throw new NullPointerException();
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

    /**
     * Gets the cause of this Event
     *
     * @return The player who triggered this event under the key: player
     */
    @Override
    public Cause getCause() {
        return Cause.builder().named("player", player).build();
    }
}
