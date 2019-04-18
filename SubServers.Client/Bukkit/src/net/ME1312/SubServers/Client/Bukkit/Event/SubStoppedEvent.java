package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Server Shell Exit Event
 */
public class SubStoppedEvent extends Event implements SubEvent {
    private String server;

    /**
     * Server Shell Exit Event
     *
     * @param server Server that Stopped
     */
    public SubStoppedEvent(String server) {
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
