package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class InternalHost extends Host {
    HashMap<String, SubServer> servers = new HashMap<String, SubServer>();

    private String name;
    private boolean enabled;
    private InetAddress address;
    private InternalSubCreator creator;
    private String directory;
    SubPlugin plugin;

    public InternalHost(SubPlugin plugin, String name, Boolean enabled, InetAddress address, String directory, String gitBash) {
        super(plugin, name, enabled, address, directory, gitBash);
        this.plugin = plugin;
        this.name = name;
        this.enabled = enabled;
        this.address = address;
        this.creator = new InternalSubCreator(this, gitBash);
        this.directory = directory;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int start(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).start(player)) i++;
        }
        return i;
    }

    @Override
    public int stop(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).stop(player)) i++;
        }
        return i;
    }

    @Override
    public int terminate(UUID player, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).terminate(player)) i++;
        }
        return i;
    }

    @Override
    public int command(UUID player, String command, String... servers) {
        int i = 0;
        for (String server : servers) {
            if (getSubServer(server.toLowerCase()).command(player, command)) i++;
        }
        return i;
    }

    @Override
    public SubCreator getCreator() {
        return creator;
    }

    @Override
    public Map<String, ? extends SubServer> getSubServers() {
        return new TreeMap<String, SubServer>(servers);
    }

    @Override
    public SubServer getSubServer(String name) {
        return servers.get(name.toLowerCase());
    }

    @Override
    public SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean hidden, boolean restricted, boolean temporary) throws InvalidServerException {
        if (plugin.api.getServers().keySet().contains(name.toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        SubServer server = new InternalSubServer(this, name, enabled, port, motd, log, directory, executable, stopcmd, start, restart, hidden, restricted, temporary);
        SubAddServerEvent event = new SubAddServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            servers.put(name.toLowerCase(), server);
            return server;
        } else {
            return null;
        }
    }

    @Override
    public boolean removeSubServer(UUID player, String name) throws InterruptedException {
        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(name));
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (getSubServer(name).isRunning()) {
                getSubServer(name).stop();
                getSubServer(name).waitFor();
            }
            servers.remove(name.toLowerCase());
            return true;
        } else return false;
    }

    @Override
    public boolean forceRemoveSubServer(UUID player, String name) {
        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(name));
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (getSubServer(name).isRunning()) {
                getSubServer(name).terminate();
            }
            servers.remove(name.toLowerCase());
            return true;
        } else return false;
    }
}
