package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.SubDataClient;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}