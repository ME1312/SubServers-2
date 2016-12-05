package net.ME1312.SubServers.Proxy.Host.Internal;

import net.ME1312.SubServers.Proxy.Host.Executable;
import net.ME1312.SubServers.Proxy.Libraries.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubCreator;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Libraries.UniversalFile;
import net.ME1312.SubServers.Proxy.SubPlugin;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class InternalHost extends Host {
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();

    private String name;
    private boolean enabled;
    private InetAddress address;
    private InternalSubCreator creator;
    UniversalFile directory;
    SubPlugin plugin;

    public InternalHost(SubPlugin plugin, String name, Boolean enabled, InetAddress address, UniversalFile directory) {
        super(plugin, name, enabled, address, directory);
        this.plugin = plugin;
        this.name = name;
        this.enabled = enabled;
        this.address = address;
        this.creator = new InternalSubCreator(this);
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
    public String getName() {
        return name;
    }

    @Override
    public void start(UUID player, String... servers) {
        for (String server : servers) {
            this.servers.get(server.toLowerCase()).start(player);
        }
    }

    @Override
    public void stop(UUID player, String... servers) {
        for (String server : servers) {
            this.servers.get(server.toLowerCase()).stop(player);
        }
    }

    @Override
    public void terminate(UUID player, String... servers) {
        for (String server : servers) {
            this.servers.get(server.toLowerCase()).terminate(player);
        }
    }

    @Override
    public void command(UUID player, String command, String... servers) {
        for (String server : servers) {
            this.servers.get(server.toLowerCase()).command(player, command);
        }
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
    public SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, Executable executable, String stopcmd, boolean start, boolean restart, boolean temporary) throws InvalidServerException {
        if (plugin.getServers().keySet().contains(name.toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        SubServer server = new InternalSubServer(this, name, enabled, port, motd, log, directory, executable, stopcmd, start, restart, temporary);
        servers.put(name.toLowerCase(), server);
        return server;
    }

    @Override
    public void removeSubServer(String name) throws InterruptedException {
        if (getSubServer(name).isRunning()) {
            getSubServer(name).stop();
            getSubServer(name).waitFor();
        }
        servers.remove(name.toLowerCase());
    }

    @Override
    public void forceRemoveSubServer(String name) {
        if (getSubServer(name).isRunning()) {
            getSubServer(name).terminate();
        }
        servers.remove(name.toLowerCase());
    }
}
