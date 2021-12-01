package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Send Command Event
 */
public class SubSendCommandEvent extends AbstractEvent implements SubEvent {
    private UUID player;
    private String server;
    private String command;
    private UUID target;

    /**
     * Server Command Event
     *
     * @param player Player Commanding
     * @param server Target Server
     * @param command Command to Send
     * @param target Player that will send
     */
    public SubSendCommandEvent(UUID player, String server, String command, UUID target) {
        Util.nullpo(server, command);
        this.player = player;
        this.server = server;
        this.command = command;
        this.target = target;
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

    /**
     * Gets the Command to Send
     *
     * @return Command to Send
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the Player that will be forced to send the Command
     *
     * @return Target Player or null if Console
     */
    public UUID getTarget() {
        return target;
    }

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
