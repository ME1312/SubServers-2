package net.ME1312.SubServers.Velocity.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubServers.Velocity.Library.SubEvent;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent implements SubEvent {
    private DataClient network;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(DataClient network) {
        if (Util.isNull(network)) throw new NullPointerException();
        this.network = network;
    }

    /**
     * Get the SubData network
     *
     * @return SubData Network
     */
    public DataClient getNetwork() {
        return network;
    }
}