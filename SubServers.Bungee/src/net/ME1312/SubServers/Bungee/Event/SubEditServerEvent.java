package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Edit Event
 */
public class SubEditServerEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private Server server;
    private Pair<String, ObjectMapValue> edit;

    /**
     * Server Edit Event
     *
     * @param player Player Adding Server
     * @param server Server to be Edited
     * @param edit Edit to make
     */
    public SubEditServerEvent(UUID player, Server server, Pair<String, ?> edit) {
        if (Util.isNull(server, edit)) throw new NullPointerException();
        ObjectMap<String> section = new ObjectMap<String>();
        section.set(".", edit.value());
        this.player = player;
        this.server = server;
        this.edit = new ContainedPair<String, ObjectMapValue>(edit.key(), section.get("."));
    }

    /**
     * Gets the Server to be Edited
     *
     * @return The Server to be Edited
     */
    public Server getServer() { return server; }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }

    /**
     * Gets the edit to be made
     *
     * @return Edit to be made
     */
    public Pair<String, ObjectMapValue> getEdit() {
        return edit;
    }

    /**
     * Gets the Cancelled Status
     *
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
