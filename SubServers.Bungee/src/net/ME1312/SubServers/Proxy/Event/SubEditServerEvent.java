package net.ME1312.SubServers.Proxy.Event;

import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Library.NamedContainer;
import net.ME1312.SubServers.Proxy.Library.SubEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SubEditServerEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private SubServer server;
    private List<NamedContainer<String, ?>> changes;

    /**
     * Server Edit Event
     *
     * @param player Player Starting Server
     * @param server Server Being Changed
     * @param change Pending Changes
     */
    public SubEditServerEvent(UUID player, SubServer server, NamedContainer<String, ?>... change) {
        this.player = player;
        this.server = server;
        this.changes = Arrays.asList(change);
    }

    /**
     * Gets the Server to be Added
     * @return The Server to be Added
     */
    public Server getServer() { return server; }

    /**
     * Gets the player that triggered the Event
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }

    /**
     * Gets the Changes to made by this Edit
     * @return Pending Changes
     */
    public List<NamedContainer<String, ?>> getChanges() {
        return changes;
    }

    /**
     * Gets the Cancelled Status
     * @return Cancelled Status
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the Cancelled Status
     */
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
