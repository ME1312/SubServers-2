package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Send Command Event
 */
public class SubSendCommandEvent extends Event implements SubEvent {
    private UUID player;
    private String server;
    private String command;

    /**
     * Server Command Event
     *
     * @param player Player Commanding Server
     * @param server Server being Commanded
     */
    public SubSendCommandEvent(UUID player, String server, String command) {
        if (Util.isNull(server, command)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.command = command;
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
     * Sets the Command to be Sent
     *
     * @param value Value
     */
    public void setCommand(String value) {
        command = value;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}
