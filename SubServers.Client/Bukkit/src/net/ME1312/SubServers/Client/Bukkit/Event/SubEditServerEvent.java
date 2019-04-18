package net.ME1312.SubServers.Client.Bukkit.Event;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Server Edit Event
 */
public class SubEditServerEvent extends Event implements SubEvent {
    private UUID player;
    private String server;
    private NamedContainer<String, ObjectMapValue<String>> edit;
    private boolean perm;

    /**
     * Server Edit Event
     *
     * @param player Player Adding Server
     * @param server Server to be Edited
     * @param edit Edit to make
     * @param permanent If the change is permanent
     */
    public SubEditServerEvent(UUID player, String server, NamedContainer<String, ?> edit, boolean permanent) {
        if (Util.isNull(server, edit)) throw new NullPointerException();
        ObjectMap<String> section = new ObjectMap<String>();
        section.set(".", edit.get());
        this.player = player;
        this.server = server;
        this.edit = new NamedContainer<String, ObjectMapValue<String>>(edit.name(), section.contains(".")?section.get("."):null);
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
    public NamedContainer<String, ObjectMapValue<String>> getEdit() {
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private static HandlerList handlers = new HandlerList();
}
