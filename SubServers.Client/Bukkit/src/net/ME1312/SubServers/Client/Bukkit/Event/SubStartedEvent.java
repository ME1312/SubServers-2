package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Server Started Event
 */
public class SubStartedEvent extends Event implements SubEvent {
    private String server;

    /**
     * Server Started Event<br>
     * <b>This event can only be called when a SubData connection is made!</b>
     *
     * @param server Server that Started
     */
    public SubStartedEvent(String server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.server = server;
    }

    /**
     * Gets the Server Effected
     *
     * @return The Server Effected
     */
    public String getServer() { return server; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}
