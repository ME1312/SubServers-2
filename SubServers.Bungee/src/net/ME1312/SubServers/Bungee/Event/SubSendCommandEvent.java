package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Command Event
 */
public class SubSendCommandEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private Server server;
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
    public SubSendCommandEvent(UUID player, Server server, String command, UUID target) {
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
    public Server getServer() { return server; }

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
     * Sets the Command to be Sent
     *
     * @param value Value
     */
    public void setCommand(String value) {
        command = value;
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
     * Gets the Cancelled Status
     *
     * @return Cancelled Status
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the Cancelled Status
     */
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
