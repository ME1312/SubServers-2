package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import net.md_5.bungee.api.plugin.Event;

/**
 * Proxy Add Event
 */
public class SubAddProxyEvent extends Event implements SubEvent {
    private Proxy proxy;

    /**
     * Proxy Add Event
     *
     * @param proxy Host Being Added
     */
    public SubAddProxyEvent(Proxy proxy) {
        if (Util.isNull(proxy)) throw new NullPointerException();
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Added
     *
     * @return The Proxy to be Added
     */
    public Proxy getProxy() { return proxy; }
}
