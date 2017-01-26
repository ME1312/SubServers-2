package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.ExtraDataHandler;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

/**
 * Host Layout Class
 */
public abstract class Host implements ExtraDataHandler {
    private YAMLSection extra = new YAMLSection();
    private String nick = null;

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
     * Get the Name of this Host
     *
     * @return Host Name
     */
    public abstract String getName();

    /**
     * Get the Display Name of this Host
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return (nick == null)?getName():nick;
    }

    /**
     * Sets the Display Name for this Host
     *
     * @param value Value (or null to reset)
     */
    public void setDisplayName(String value) {
        this.nick = value;
    }

    /**
     * Starts the Servers Specified
     *
     * @param servers Servers
     * @return Success Status
     */
    public int start(String... servers) {
        return start(null, servers);
    }

    /**
     * Starts the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     * @return Success Status
     */
    public abstract int start(UUID player, String... servers);

    /**
     * Stops the Servers Specified
     *
     * @param servers Servers
     * @return Success Status
     */
    public int stop(String... servers) {
        return stop(null, servers);
    }

    /**
     * Stops the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     * @return Success Status
     */
    public abstract int stop(UUID player, String... servers);

    /**
     * Terminates the Servers Specified
     *
     * @param servers Servers
     * @return Success Status
     */
    public int terminate(String... servers) {
        return terminate(null, servers);
    }

    /**
     * Terminates the Servers Specified
     *
     * @param player Player who started
     * @param servers Servers
     * @return Success Status
     */
    public abstract int terminate(UUID player, String... servers);

    /**
     * Commands the Servers Specified
     *
     * @param command Command to send
     * @param servers Servers
     * @return Success Status
     */
    public int command(String command, String... servers) {
        return command(null, command, servers);
    }

    /**
     * Commands the Servers Specified
     *
     * @param player Player who started
     * @param command Command to send
     * @param servers Servers
     * @return Success Status
     */
    public abstract int command(UUID player, String command, String... servers);

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
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param temporary Temporary Status
     * @return The SubServer
     * @throws InvalidServerException
     */
    public abstract SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException;

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
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param temporary Temporary Status
     * @return The SubServer
     * @throws InvalidServerException
     */
    public SubServer addSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException {
        return addSubServer(null, name, enabled, port, motd, log, directory, executable, stopcmd, start, restart, hidden, restricted, temporary);
    }

    /**
     * Removes a SubServer
     *
     * @param name SubServer Name
     * @throws InterruptedException
     * @return Success Status
     */
    public boolean removeSubServer(String name) throws InterruptedException {
        return removeSubServer(null, name);
    };

    /**
     * Removes a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @throws InterruptedException
     * @return Success Status
     */
    public abstract boolean removeSubServer(UUID player, String name) throws InterruptedException;

    /**
     * Forces the Removal of a SubServer
     *
     * @param name SubServer Name
     */
    public boolean forceRemoveSubServer(String name) {
        return forceRemoveSubServer(null, name);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     */
    public abstract boolean forceRemoveSubServer(UUID player, String name);

    @Override
    public void addExtra(String handle, Object value) {
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        return extra.getKeys().contains(handle);
    }

    @Override
    public YAMLValue getExtra(String handle) {
        return extra.get(handle);
    }

    @Override
    public YAMLSection getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        extra.remove(handle);
    }
}
