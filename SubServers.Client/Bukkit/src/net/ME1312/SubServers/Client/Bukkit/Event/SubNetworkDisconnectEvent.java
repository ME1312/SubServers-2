package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends Event implements SubEvent {
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}