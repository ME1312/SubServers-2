package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.md_5.bungee.api.plugin.Event;

/**
 * Proxy Remove Event
 */
public class SubRemoveProxyEvent extends Event implements SubEvent {
    private Proxy proxy;

    /**
     * Proxy Remove Event
     *
     * @param proxy Host Being Added
     */
    public SubRemoveProxyEvent(Proxy proxy) {
        if (Util.isNull(proxy)) throw new NullPointerException();
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Removed
     *
     * @return The Proxy to be Removed
     */
    public Proxy getProxy() { return proxy; }
}
