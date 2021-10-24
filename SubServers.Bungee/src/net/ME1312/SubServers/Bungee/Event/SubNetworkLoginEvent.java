package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.DataServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Login Event
 */
public class SubNetworkLoginEvent extends Event implements SubEvent {
    private DataServer network;
    private DataClient client;

    /**
     * SubData Network Login Event
     */
    public SubNetworkLoginEvent(DataServer network, DataClient client) {
        Util.nullpo(network, client);
        this.network = network;
        this.client = client;
    }

    /**
     * Get the network the client is connected to
     *
     * @return SubData Network
     */
    public DataServer getNetwork() {
        return network;
    }

    /**
     * Get the connecting client
     *
     * @return Client
     */
    public DataClient getClient() {
        return client;
    }
}