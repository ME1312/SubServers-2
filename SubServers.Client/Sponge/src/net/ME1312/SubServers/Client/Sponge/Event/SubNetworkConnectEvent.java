package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends AbstractEvent implements SubEvent {
    private DataClient network;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(DataClient network) {
        Util.nullpo(network);
        this.network = network;
    }

    /**
     * Get the SubData network
     *
     * @return SubData Network
     */
    public DataClient getNetwork() {
        return network;
    }

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