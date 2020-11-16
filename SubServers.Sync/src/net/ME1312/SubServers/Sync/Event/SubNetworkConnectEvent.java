package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Sync.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends Event implements SubEvent {
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