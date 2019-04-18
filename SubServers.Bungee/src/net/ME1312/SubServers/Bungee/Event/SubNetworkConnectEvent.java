package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.DataServer;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataServer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.net.InetAddress;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private DataServer network;
    private DataClient client;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(DataServer network, DataClient client) {
        if (Util.isNull(network, client)) throw new NullPointerException();
        this.network = network;
        this.client = client;
    }

    /**
     * Get the network the client is trying to connect to
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

    /**
     * Gets the Cancelled Status
     *
     * @return Cancelled Status
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the Cancelled Status
     */
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}