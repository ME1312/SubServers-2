package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * Proxy Remove Event
 */
public class SubRemoveProxyEvent extends AbstractEvent implements SubEvent {
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

    /**
     * Gets the cause of this Event
     *
     * @deprecated Use simplified methods where available
     * @return An empty cause list
     */
    @Override
    @Deprecated
    public Cause getCause() {
        return Cause.builder().build(getContext());
    }
}
