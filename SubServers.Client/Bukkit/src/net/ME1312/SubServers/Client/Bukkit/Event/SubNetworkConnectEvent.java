package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}