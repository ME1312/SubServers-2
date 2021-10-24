package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Create Event
 */
public class SubCreateEvent extends Event implements SubEvent, Cancellable {
    private boolean cancelled = false;
    private UUID player;
    private SubServer update;
    private Host host;
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;

    /**
     * Server Create Event
     *
     * @param player Player Creating
     * @param host Potential Host
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     */
    public SubCreateEvent(UUID player, Host host, String name, SubCreator.ServerTemplate template, Version version, int port) {
        Util.nullpo(host, name, template, port);
        this.player = player;
        this.host = host;
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
    }

    /**
     * Server Create Event (as an Update)
     *
     * @param player Player Updating
     * @param server Server to be Updated
     * @param template Server Template
     * @param version Server Version
     */
    public SubCreateEvent(UUID player, SubServer server, SubCreator.ServerTemplate template, Version version) {
        Util.nullpo(server);
        this.player = player;
        this.update = server;
        this.name = server.getName();
        this.host = server.getHost();
        this.template = template;
        this.version = version;
        this.port = server.getAddress().getPort();
    }

    /**
     * Get the Host the SubServer will run on
     *
     * @return Potential Host
     */
    public Host getHost() {
        return host;
    }

    /**
     * Get if SubCreator is being run in update mode
     *
     * @return Update Mode Status
     */
    public boolean isUpdate() {
        return update != null;
    }

    /**
     * Get the Server that's being updated
     *
     * @return Updating Server
     */
    public SubServer getUpdatingServer() {
        return update;
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
    public SubCreator.ServerTemplate getTemplate() {
        return template;
    }

    /**
     * Set the Template to Use
     *
     * @param value Value
     */
    public void setTemplate(SubCreator.ServerTemplate value) {
        this.template = value;
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
     * Set the Version the Server will use
     *
     * @param value Value
     */
    public void setVersion(Version value) {
        this.version = value;
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
