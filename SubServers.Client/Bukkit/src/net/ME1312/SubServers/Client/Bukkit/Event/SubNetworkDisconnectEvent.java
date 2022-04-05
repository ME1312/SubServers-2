package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event implements SubEvent {
    private DataClient network;
    private DisconnectReason reason;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkDisconnectEvent(DataClient network, DisconnectReason reason) {
        super(true);
        Util.nullpo(network, reason);
        this.network = network;
        this.reason = reason;
    }

    /**
     * Get the SubData network
     *
     * @return SubData Network
     */
    public DataClient getNetwork() {
        return network;
    }

    /**
     * Get the reason the client disconnected
     *
     * @return Disconnect Reason
     */
    public DisconnectReason getReason() {
        return reason;
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