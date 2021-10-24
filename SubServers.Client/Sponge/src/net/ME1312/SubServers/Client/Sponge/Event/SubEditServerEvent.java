package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Server Edit Event
 */
public class SubEditServerEvent extends AbstractEvent implements SubEvent {
    private UUID player;
    private String server;
    private Pair<String, ObjectMapValue<String>> edit;

    /**
     * Server Edit Event
     *
     * @param player Player Adding Server
     * @param server Server to be Edited
     * @param edit Edit to make
     */
    public SubEditServerEvent(UUID player, String server, Pair<String, ?> edit) {
        Util.nullpo(server, edit);
        ObjectMap<String> section = new ObjectMap<String>();
        section.set(".", edit.value());
        this.player = player;
        this.server = server;
        this.edit = new ContainedPair<String, ObjectMapValue<String>>(edit.key(), section.contains(".")?section.get("."):null);
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
     * Gets the cause of this Event
     *
     * @deprecated Use simplified methods where available
     * @return The player UUID who triggered this event
     */
    @Override
    @Deprecated
    public Cause getCause() {
        return Cause.builder().append(player).build(getContext());
    }
}
