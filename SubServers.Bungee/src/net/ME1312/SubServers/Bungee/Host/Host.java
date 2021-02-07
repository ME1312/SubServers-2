package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.ExtraDataHandler;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import com.google.common.collect.Range;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.util.*;

/**
 * Host Layout Class
 */
public abstract class Host implements ExtraDataHandler {
    private ObjectMap<String> extra = new ObjectMap<String>();
    private final String signature;
    private String nick = null;

    /**
     * This constructor is required to launch your host from the drivers list. Do not add or remove any arguments.
     *
     * @param plugin SubServers Internals
     * @param name The Name of your Host
     * @param ports The range of ports to auto-select from
     * @param log Whether apps like SubCreator should log to console (does not apply to servers)
     * @param enabled If your host is Enabled
     * @param address The address of your Host
     * @param directory The runtime directory of your Host
     * @param gitBash The Git Bash directory
     */
    @SuppressWarnings("deprecation")
    public Host(SubProxy plugin, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) {
        if (name.contains(" ")) throw new InvalidHostException("Host names cannot have spaces: " + name);
        if (!ports.hasLowerBound() || !ports.hasUpperBound()) throw new InvalidHostException("Port range is not bound");
        if (Util.isNull(plugin, name, enabled, ports, log, address, directory, gitBash)) throw new NullPointerException();
        signature = plugin.api.signAnonymousObject();
        SubAPI.getInstance().getInternals().subprotocol.whitelist(address.getHostAddress());
    }

    /**
     * Is this Host Available?
     *
     * @return Availability Status
     */
    public boolean isAvailable() {
        return true;
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
     * Get players on servers provided by this host
     *
     * @return Local Player Collection
     */
    public Collection<ProxiedPlayer> getPlayers() {
        LinkedList<ProxiedPlayer> players = new LinkedList<ProxiedPlayer>();
        for (SubServer server : getSubServers().values()) {
            players.addAll(server.getPlayers());
        }
        return players;
    }

    /**
     * Get players on servers provided by this host across all known proxies
     *
     * @return Remote Player Collection
     */
    public Collection<RemotePlayer> getRemotePlayers() {
        LinkedList<RemotePlayer> players = new LinkedList<RemotePlayer>();
        for (SubServer server : getSubServers().values()) {
            players.addAll(server.getRemotePlayers());
        }
        return players;
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
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @return The SubServer
     * @throws InvalidServerException
     */
    public abstract SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException;

    /**
     * Adds a SubServer
     *
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @return The SubServer
     * @throws InvalidServerException
     */
    public SubServer addSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        return addSubServer(null, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
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
    }

    /**
     * Removes a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @throws InterruptedException
     * @return Success Status
     */
    public boolean removeSubServer(UUID player, String name) throws InterruptedException {
        return removeSubServer(player, name, false);
    }

    /**
     * Removes a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @param forced Forces the Removal
     * @throws InterruptedException
     * @return Success Status
     */
    protected abstract boolean removeSubServer(UUID player, String name, boolean forced) throws InterruptedException;

    /**
     * Forces the Removal of a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceRemoveSubServer(String name) throws InterruptedException {
        return forceRemoveSubServer(null, name);
    }

    /**
     * Forces the Removal of a SubServer
     *
     * @param player Player Removing
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceRemoveSubServer(UUID player, String name) throws InterruptedException {
        return removeSubServer(player, name, true);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean recycleSubServer(String name) throws InterruptedException {
        return recycleSubServer(null, name);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean recycleSubServer(UUID player, String name) throws InterruptedException {
        return recycleSubServer(player, name, false);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param forced Forces the Deletion
     * @return Success Status
     */
    protected abstract boolean recycleSubServer(UUID player, String name, boolean forced) throws InterruptedException;

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceRecycleSubServer(String name) throws InterruptedException {
        return forceRecycleSubServer(null, name);
    }

    /**
     * Forces the Deletion of a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceRecycleSubServer(UUID player, String name) throws InterruptedException {
        return recycleSubServer(player, name, true);
    }

    /**
     * Deletes a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean deleteSubServer(String name) throws InterruptedException {
        return deleteSubServer(null, name);
    }

    /**
     * Deletes a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean deleteSubServer(UUID player, String name) throws InterruptedException {
        return deleteSubServer(player, name, false);
    }

    /**
     * Deletes a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param forced Forces the Deletion
     * @return Success Status
     */
    protected abstract boolean deleteSubServer(UUID player, String name, boolean forced) throws InterruptedException;

    /**
     * Forces the Deletion of a SubServer
     *
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceDeleteSubServer(String name) throws InterruptedException {
        return forceDeleteSubServer(null, name);
    }

    /**
     * Forces the Deletion of a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @return Success Status
     */
    public boolean forceDeleteSubServer(UUID player, String name) throws InterruptedException {
        return deleteSubServer(player, name, true);
    }

    /**
     * Resets this Host object
     *
     * @return Success Status
     */
    @SuppressWarnings("unchecked")
    public boolean destroy() {
        try {
            Map.Entry<String, SubServer>[] subservers = getSubServers().entrySet().toArray(new Map.Entry[0]);

            for (Map.Entry<String, SubServer> entry : subservers) {
                if (entry.getValue().isRunning()) Logger.get("SubServers").info("Stopping " + entry.getValue().getName());
                forceRemoveSubServer(entry.getKey());
            }
            getCreator().terminate();
            getCreator().waitFor();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the Signature of this Object
     *
     * @return Object Signature
     */
    public final String getSignature() {
        return signature;
    }

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
    public ObjectMapValue getExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return extra.get(handle);
    }

    @Override
    public ObjectMap<String> getExtra() {
        return extra.clone();
    }

    @Override
    public void removeExtra(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        extra.remove(handle);
    }

    public ObjectMap<String> forSubData() {
        ObjectMap<String> hinfo = new ObjectMap<String>();
        hinfo.set("type", "Host");
        hinfo.set("name", getName());
        hinfo.set("display", getDisplayName());
        hinfo.set("available", isAvailable());
        hinfo.set("enabled", isEnabled());
        hinfo.set("address", getAddress().getHostAddress());
        hinfo.set("dir", getPath());

        ObjectMap<String> cinfo = new ObjectMap<String>();
        ObjectMap<String> templates = new ObjectMap<String>();
        for (SubCreator.ServerTemplate template : getCreator().getTemplates().values())
            templates.set(template.getName(), template.forSubData());
        cinfo.set("templates", templates);
        hinfo.set("creator", cinfo);

        ObjectMap<String> servers = new ObjectMap<String>();
        for (SubServer server : getSubServers().values()) {
            servers.set(server.getName(), server.forSubData());
        }
        hinfo.set("servers", servers);
        hinfo.set("signature", signature);
        hinfo.set("extra", getExtra());
        return hinfo;
    }
}
