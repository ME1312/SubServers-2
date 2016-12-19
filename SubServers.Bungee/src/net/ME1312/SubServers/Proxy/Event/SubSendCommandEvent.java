package net.ME1312.SubServers.Proxy.Event;

import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.SubEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

public class SubSendCommandEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private SubServer server;
    private String command;

    /**
     * Server Command Event
     *
     * @param player Player Commanding Server
     * @param server Server being Commanded
     */
    public SubSendCommandEvent(UUID player, SubServer server, String command) {
        this.player = player;
        this.server = server;
        this.command = command;
    }

    /**
     * Gets the Server Effected
     * @return The Server Effected
     */
    public SubServer getServer() { return server; }

    /**
     * Gets the player that triggered the Event
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
     * Gets the Cancelled Status
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
