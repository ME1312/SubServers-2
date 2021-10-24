package net.ME1312.SubServers.Client.Sponge.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Common.Network.API.SubServer;
import net.ME1312.SubServers.Client.Sponge.Library.SubEvent;
import net.ME1312.SubServers.Client.Sponge.SubAPI;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server Create Event
 */
public class SubCreateEvent extends AbstractEvent implements SubEvent {
    private UUID player;
    private boolean update;
    private String host;
    private String name;
    private String template;
    private Version version;
    private int port;

    /**
     * Server Create Event
     *
     * @param player Player Creating
     * @param host Potential Host
     * @param name Server Name
     * @param template Server Type
     * @param version Server Version
     * @param port Server Port Number
     */
    public SubCreateEvent(UUID player, String host, String name, String template, Version version, int port, boolean update) {
        Util.nullpo(host, name, template, port);
        this.player = player;
        this.update = update;
        this.host = host;
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
    }

    /**
     * Get the Host the SubServer will run on
     *
     * @return Potential Host
     */
    public String getHost() {
        return host;
    }

    /**
     * Get if SubCreator is being run in update mode
     *
     * @return Update Mode Status
     */
    public boolean isUpdate() {
        return update;
    }

    /**
     * Get the Server that's being updated
     *
     * @param callback Updating Server
     */
    public void getUpdatingServer(Consumer<SubServer> callback) {
        if (!update) {
            try {
                callback.accept(null);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.printStackTrace();
            }
        } else {
            SubAPI.getInstance().getSubServer(name, callback);
        }
    }

    /**
     * Get the name the SubServer will use
     *
     * @return SubServer Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Template to Use
     *
     * @return Server Template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Get the Version the Server will use
     *
     * @return Server Version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Get the Port the Server will use
     *
     * @return Port Number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the player that triggered the Event
     *
     * @return The Player that triggered this Event or null if Console
     */
    public UUID getPlayer() { return player; }

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
