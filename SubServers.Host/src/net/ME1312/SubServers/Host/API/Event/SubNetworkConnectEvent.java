package net.ME1312.SubServers.Host.API.Event;

import net.ME1312.SubServers.Host.Library.Event.Event;
import net.ME1312.SubServers.Host.Network.SubDataClient;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkConnectEvent extends Event {
    private SubDataClient network;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkConnectEvent(SubDataClient network) {
        this.network = network;
    }
}