package net.ME1312.SubServers.Sync.Event;

import net.ME1312.SubServers.Sync.Library.SubEvent;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.md_5.bungee.api.plugin.Event;

import java.util.UUID;

/**
 * Server Create Event
 */
public class SubCreateEvent extends Event implements SubEvent {
    private UUID player;
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
    public SubCreateEvent(UUID player, String host, String name, String template, Version version, int port) {
        if (Util.isNull(host, name, template, version, port)) throw new NullPointerException();
        this.player = player;
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
     * Set the Template to Use
     *
     * @param value Value
     */
    public void getTemplate(String value) {
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
}
