package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubServers.Sync.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event implements SubEvent {
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