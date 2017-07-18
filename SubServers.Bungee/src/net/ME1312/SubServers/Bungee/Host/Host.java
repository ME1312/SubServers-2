package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLValue;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.ExtraDataHandler;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
        if (name.equals("~")) nick = "Default";
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
     * Get the host Directory Path
     *
     * @return Host Directory Path
     */
    public abstract String getPath();

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
        if (value == null || value.length() == 0 || getName().equals(value)) {
            this.nick = null;
        } else {
            this.nick = value;
        }
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
    public int start(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).start(player)) i++;
        }
        return i;
    }

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
    public int stop(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).stop(player)) i++;
        }
        return i;
    }

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
    public int terminate(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).terminate(player)) i++;
        }
        return i;
    }

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
    public int command(UUID player, String command, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).command(player, command)) i++;
        }
        return i;
    }

    /**
     * Edits the Host
     *
     * @param player Player Editing
     * @param edit Edits
     * @return Success Status
     */
    public abstract int edit(UUID player, YAMLSection edit);

    /**
     * Edits the Host
     *
     * @param edit Edits
     * @return Success Status
     */
    public int edit(YAMLSection edit) {
        return edit(null, edit);
    }

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
    public boolean forceRemoveSubServer(String name) throws InterruptedException {
        return forceRemoveSubServer(null, name);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     */
    public abstract boolean forceRemoveSubServer(UUID player, String name) throws InterruptedException;

    /**
     * Delete a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean deleteSubServer(String name) throws InterruptedException {
        return deleteSubServer(null, name);
    }

    /**
     * Delete a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public abstract boolean deleteSubServer(UUID player, String name) throws InterruptedException;

    /**
     * Forced the Deletion of a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceDeleteSubServer(String name) throws InterruptedException {
        return deleteSubServer(null, name);
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public abstract boolean forceDeleteSubServer(UUID player, String name) throws InterruptedException;

    @Override
    public void addExtra(String handle, Object value) {
        if (Util.isNull(handle, value)) throw new NullPointerException();
        extra.set(handle, value);
    }

    @Override
    public boolean hasExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.getKeys().contains(handle);
    }

    @Override
    public YAMLValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.get(handle);
    }

    @Override
    public YAMLSection getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        extra.remove(handle);
    }

    @Override
    public String toString() {
        JSONObject hinfo = new JSONObject();
        hinfo.put("type", "Host");
        hinfo.put("name", getName());
        hinfo.put("enabled", isEnabled());
        hinfo.put("display", getDisplayName());
        JSONObject servers = new JSONObject();
        for (SubServer server : getSubServers().values()) {
            servers.put(server.getName(), server.toString());
        }
        hinfo.put("servers", servers);
        if (this instanceof ClientHandler && ((ClientHandler) this).getSubData() != null) hinfo.put("subdata", ((ClientHandler) this).getSubData().getAddress().toString());
        hinfo.put("extra", getExtra().toJSON());
        return hinfo.toString();
    }
}
