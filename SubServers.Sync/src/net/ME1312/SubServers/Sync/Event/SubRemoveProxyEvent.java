package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * Proxy Remove Event
 */
public class SubRemoveProxyEvent extends Event implements SubEvent {
    private String proxy;

    /**
     * Proxy Remove Event
     *
     * @param proxy Host Being Added
     */
    public SubRemoveProxyEvent(String proxy) {
        Util.nullpo(proxy);
        this.proxy = proxy;
    }

    /**
     * Gets the Proxy to be Removed
     *
     * @return The Proxy to be Removed
     */
    public String getProxy() { return proxy; }
}
