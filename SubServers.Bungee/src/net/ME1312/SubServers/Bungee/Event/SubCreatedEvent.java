package net.ME1312.SubServers.Bungee.Event;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.SubEvent;

import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Created Event
 */
public class SubCreatedEvent extends Event implements SubEvent {
    private UUID player;
    private SubServer server;
    private boolean success;
    private boolean update;
    private Host host;
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;

    /**
     * Server Created Event
     *
     * @param player Player Creating
     * @param host Potential Host
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     * @param server Server Object
     * @param update Update Mode Status
     * @param success Success Status
     */
    public SubCreatedEvent(UUID player, Host host, String name, SubCreator.ServerTemplate template, Version version, int port, SubServer server, boolean update, boolean success) {
        if (Util.isNull(host, name, template, port)) throw new NullPointerException();
        this.player = player;
        this.host = host;
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
        this.server = server;
        this.update = update;
        this.success = success;
    }

    /**
     * Get the Host the SubServer runs on
     *
     * @return Host
     */
    public Host getHost() {
        return host;
    }

    /**
     * Get if SubCreator was being run in update mode
     *
     * @return Update Mode Status
     */
    public boolean wasUpdate() {
        return update;
    }

    /**
     * Get if the operation was a success
     *
     * @return Success Status
     */
    public boolean wasSuccessful() {
        return success;
    }

    /**
     * Get the Server that was created/updated
     *
     * @return Finished Server
     */
    public SubServer getServer() {
        return server;
    }

    /**
     * Get the name the SubServer used
     *
     * @return SubServer Name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Template that was used
     *
     * @return Server Template
     */
    public SubCreator.ServerTemplate getTemplate() {
        return template;
    }

    /**
     * Get the Version the Server used
     *
     * @return Server Version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Get the Port the Server used
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
}
