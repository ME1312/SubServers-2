package net.ME1312.SubServers.Sync.Event;

import net.ME1312.SubServers.Sync.Library.SubEvent;
import net.ME1312.SubServers.Sync.Library.Util;
import net.md_5.bungee.api.plugin.Event;

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
        if (Util.isNull(proxy)) throw new NullPointerException();
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Added
     *
     * @return The Proxy to be Added
     */
    public String getProxy() { return proxy; }
}
