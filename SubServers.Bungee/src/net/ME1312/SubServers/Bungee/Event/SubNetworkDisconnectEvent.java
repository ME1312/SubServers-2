package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event implements SubEvent {
    private SubDataServer network;
    private Client client;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkDisconnectEvent(SubDataServer network, Client client) {
        if (Util.isNull(network, client)) throw new NullPointerException();
        this.network = network;
        this.client = client;
    }

    /**
     * Get the network the client is disconnecting from
     *
     * @return SubData Network
     */
    public SubDataServer getNetwork() {
        return network;
    }

    /**
     * Get the disconnecting client
     *
     * @return Client
     */
    public Client getClient() {
        return client;
    }

}