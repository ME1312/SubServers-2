package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends AbstractEvent implements SubEvent {
    private DataClient network;
    private DisconnectReason reason;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkDisconnectEvent(DataClient network, DisconnectReason reason) {
        if (Util.isNull(network, reason)) throw new NullPointerException();
        this.network = network;
        this.reason = reason;
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
     * Get the reason the client disconnected
     *
     * @return Disconnect Reason
     */
    public DisconnectReason getReason() {
        return reason;
    }

    /**
     * Gets the cause of this Event
     *
     * @deprecated Use simplified methods where available
     * @return Disconnect Reason
     */
    @Override
    @Deprecated
    public Cause getCause() {
        return Cause.builder().append(reason).build(getContext());
    }
}