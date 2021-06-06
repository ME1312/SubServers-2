package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends Event implements SubEvent {
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}