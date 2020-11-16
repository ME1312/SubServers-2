package net.ME1312.SubServers.Sync.Event;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Sync.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Edit Event
 */
public class SubEditServerEvent extends Event implements SubEvent {
    private UUID player;
    private String server;
    private Pair<String, ObjectMapValue<String>> edit;
    private boolean perm;

    /**
     * Server Edit Event
     *
     * @param player Player Adding Server
     * @param server Server to be Edited
     * @param edit Edit to make
     * @param permanent If the change is permanent
     */
    public SubEditServerEvent(UUID player, String server, Pair<String, ?> edit, boolean permanent) {
        if (Util.isNull(server, edit)) throw new NullPointerException();
        ObjectMap<String> section = new ObjectMap<String>();
        section.set(".", edit.value());
        this.player = player;
        this.server = server;
        this.edit = new ContainedPair<String, ObjectMapValue<String>>(edit.key(), section.get("."));
        this.perm = permanent;
    }

    /**
     * Gets the Server to be Edited
     *
     * @return The Server to be Edited
     */
    public String getServer() { return server; }

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
    public Pair<String, ObjectMapValue<String>> getEdit() {
        return edit;
    }

    /**
     * Gets if the edit is permanent
     *
     * @return Permanent Status
     */
    public boolean isPermanent() {
        return perm;
    }
}
