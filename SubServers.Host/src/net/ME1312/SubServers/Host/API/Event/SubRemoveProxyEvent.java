package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;

/**
 * Proxy Remove Event
 */
public class SubRemoveProxyEvent extends Event {
    private String proxy;

    /**
     * Proxy Remove Event
     *
     * @param proxy Host Being Added
     */
    public SubRemoveProxyEvent(String proxy) {
        if (Util.isNull(proxy)) throw new NullPointerException();
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Removed
     *
     * @return The Proxy to be Removed
     */
    public String getProxy() { return proxy; }
}
