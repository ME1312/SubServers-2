package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Library.Event.Event;
import net.ME1312.Galaxi.Library.Util;

/**
 * Proxy Add Event
 */
public class SubAddProxyEvent extends Event {
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
