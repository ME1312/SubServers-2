package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Network.SubDataClient;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends Event {
    private SubDataClient network;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(SubDataClient network) {
        if (Util.isNull(network)) throw new NullPointerException();
        this.network = network;
    }

    /**
     * Get the SubData network
     *
     * @return SubData Network
     */
    public SubDataClient getNetwork() {
        return network;
    }
}