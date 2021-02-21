package net.ME1312.SubServers.Host.Event;

import net.ME1312.Galaxi.Event.Event;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.DisconnectReason;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event {
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
}