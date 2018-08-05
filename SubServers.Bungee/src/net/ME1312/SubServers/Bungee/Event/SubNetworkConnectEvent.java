package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.net.InetAddress;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private SubDataServer network;
    private InetAddress address;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(SubDataServer network, InetAddress address) {
        if (Util.isNull(network, address)) throw new NullPointerException();
        this.network = network;
        this.address = address;
    }

    /**
     * Get the network the client is trying to connect to
     *
     * @return SubData Network
     */
    public SubDataServer getNetwork() {
        return network;
    }

    /**
     * Get the address of the connecting client
     *
     * @return Client address
     */
    public InetAddress getAddress() {
        return address;
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