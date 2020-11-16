package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.DataServer;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event implements SubEvent {
    private DataServer network;
    private DataClient client;
    private DisconnectReason reason;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkDisconnectEvent(DataServer network, DataClient client, DisconnectReason reason) {
        if (Util.isNull(network, client, reason)) throw new NullPointerException();
        this.network = network;
        this.client = client;
        this.reason = reason;
    }

    /**
     * Get the network the client is disconnecting from
     *
     * @return SubData Network
     */
    public DataServer getNetwork() {
        return network;
    }

    /**
     * Get the disconnecting client
     *
     * @return Client
     */
    public DataClient getClient() {
        return client;
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