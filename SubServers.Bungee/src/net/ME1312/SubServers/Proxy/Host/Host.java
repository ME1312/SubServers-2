package net.ME1312.SubServers.Proxy.Host;

import net.ME1312.SubServers.Proxy.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Proxy.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Library.NamedContainer;
import net.ME1312.SubServers.Proxy.SubPlugin;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

/**
 * Host Layout Class
 *
 * @author ME1312
 */
public abstract class Host {

    /**
     * This constructor is required to launch your host from the drivers list. Do not add or remove any arguments.
     *
     * @param plugin SubServers Internals
     * @param name The Name of your Host
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     */
    public Host(SubPlugin plugin, String name, Boolean enabled, InetAddress address, String directory, String gitBash) {
        if (name.contains(" ")) throw new InvalidHostException("Host names cannot have spaces: " + name);
    }

    /**
     * Is this Host Enabled?
     *
     * @return Enabled Status
     */
    public abstract boolean isEnabled();

    /**
     * Set if this Host is Enabled
     *
     * @param value Value
     */
    public abstract void setEnabled(boolean value);

    /**
     * Get the Address of this Host
     *
     * @return Host Address
     */
    public abstract InetAddress getAddress();

    /**
     * Get the Directory of this Host
     *
     * @return Host Directory
     */
    public abstract String getDirectory();

    /**
     * Get if the Host can be Edited
     *
     * @return Editable Status
     */
    public abstract boolean isEditable();

    /**
     * Get the Name of this Host
     *
     * @return Host Name
     */
    public abstract String getName();

    /**
     * Starts the Servers Specified
     *
     * @param servers Servers
     */
    public void start(String... servers) {
        start(null, servers);
    }

    /**
     * Starts the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public abstract void start(UUID player, String... servers);

    /**
     * Stops the Servers Specified
     *
     * @param servers Servers
     */
    public void stop(String... servers) {
        stop(null, servers);
    }

    /**
     * Stops the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public abstract void stop(UUID player, String... servers);

    /**
     * Terminates the Servers Specified
     *
     * @param servers Servers
     */
    public void terminate(String... servers) {
        terminate(null, servers);
    }

    /**
     * Terminates the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     */
    public abstract void terminate(UUID player, String... servers);

    /**
     * Commands the Servers Specified
     *
     * @param command Command to send
     * @param servers Servers
     */
    public void command(String command, String... servers) {
        command(null, command, servers);
    }

    /**
     * Commands the Servers Specified
     *
     * @param player Player who started
     * @param command Command to send
     * @param servers Servers
     */
    public abstract void command(UUID player, String command, String... servers);

    /**
     * Applies edits to the Host
     *
     * @param change Change(s) to be applied
     */
    public abstract void edit(NamedContainer<String, ?>... change);

    /**
     * Gets the SubCreator Instance for this Host
     *
     * @return SubCreator
     */
    public abstract SubCreator getCreator();

    /**
     * Gets the SubServers on this Host
     *
     * @return SubServer Map
     */
    public abstract Map<String, ? extends SubServer> getSubServers();

    /**
     * Gets a SubServer
     *
     * @param name SubServer Name
     * @return a SubServer
     */
    public abstract SubServer getSubServer(String name);

    /**
     * Adds a SubServer
     *
     * @param player Player who Added
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable
     * @param stopcmd Command to Stop the Server
     * @param restart Auto Restart Status
     * @param restricted Players will need a permission to join if true
     * @param temporary Temporary Status
     * @return The SubServer
     * @throws InvalidServerException
     */
    public abstract SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean restricted, boolean temporary) throws InvalidServerException;

    /**
     * Adds a SubServer
     *
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable
     * @param stopcmd Command to Stop the Server
     * @param restart Auto Restart Status
     * @param restricted Players will need a permission to join if true
     * @param temporary Temporary Status
     * @return The SubServer
     * @throws InvalidServerException
     */
    public SubServer addSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean restricted, boolean temporary) throws InvalidServerException {
        return addSubServer(null, name, enabled, port, motd, log, directory, executable, stopcmd, start, restart, restricted, temporary);
    }

    /**
     * Removes a SubServer
     *
     * @param name SubServer Name
     * @throws InterruptedException
     */
    public abstract void removeSubServer(String name) throws InterruptedException;

    /**
     * Forces the Removal of a SubServer
     *
     * @param name SubServer Name
     */
    public abstract void forceRemoveSubServer(String name);

}
