package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Proxy Add Event
 */
public class SubAddProxyEvent extends Event implements SubEvent {
    private String proxy;

    /**
     * Proxy Add Event
     *
     * @param proxy Host Being Added
     */
    public SubAddProxyEvent(String proxy) {
        super(true);
        Util.nullpo(proxy);
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Added
     *
     * @return The Proxy to be Added
     */
    public String getProxy() { return proxy; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}
