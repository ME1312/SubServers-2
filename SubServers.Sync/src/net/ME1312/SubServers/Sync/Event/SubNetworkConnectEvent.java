package net.ME1312.SubServers.Sync.Event;

import net.ME1312.SubServers.Sync.Library.SubEvent;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.md_5.bungee.api.plugin.Event;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkConnectEvent extends Event implements SubEvent {
    private SubDataClient network;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkConnectEvent(SubDataClient network) {
        this.network = network;
    }
}