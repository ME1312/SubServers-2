package net.ME1312.SubServers.Bungee.Host.External;

import com.google.common.collect.Range;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Callback;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExAddServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDeleteServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExRemoveServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutReset;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.net.InetAddress;
import java.util.*;

/**
 * External Host Class
 */
public class ExternalHost extends Host implements ClientHandler {
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private String name;
    private boolean enabled;
    private InetAddress address;
    private SubCreator creator;
    private String directory;
    protected NamedContainer<Boolean, Client> client;
    private LinkedList<PacketOut> queue;
    private boolean clean;
    protected SubPlugin plugin;

    /**
     * Creates an External Host
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
    public ExternalHost(SubPlugin plugin, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) {
        super(plugin, name, enabled, ports, log, address, directory, gitBash);
        this.plugin = plugin;
        this.name = name;
        this.enabled = enabled;
        this.address = address;
        this.client = new NamedContainer<Boolean, Client>(false, null);
        this.creator = new ExternalSubCreator(this, ports, log, gitBash);
        this.directory = directory;
        this.queue = new LinkedList<PacketOut>();
        this.clean = false;
    }

    @Override
    public Client getSubData() {
        return client.get();
    }

    @Override
    public void setSubData(Client client) {
        this.client = new NamedContainer<Boolean, Client>(false, client);
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) client.setHandler(this);
    }

    protected void queue(PacketOut... packet) {
        for (PacketOut p : packet) if (client.get() == null || client.name() == false) {
            queue.add(p);
        } else {
            client.get().sendPacket(p);
        }
    }
    private void requeue() {
        if (!clean) {
            client.get().sendPacket(new PacketOutReset("Prevent Desync"));
            clean = true;
        }
        for (SubServer server : servers.values()) {
            client.get().sendPacket(new PacketExAddServer(server.getName(), server.isEnabled(), server.getAddress().getPort(), server.isLogging(), server.getPath(), ((ExternalSubServer) server).exec, server.getStopCommand(), (server.isRunning())?((ExternalSubLogger) server.getLogger()).getExternalAddress():null));
        }
        while (queue.size() != 0) {
            client.get().sendPacket(queue.get(0));
            queue.remove(0);
        }
        client.rename(true);
    }

    @Override
    public boolean isAvailable() {
        return this.client.name();
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
    public String getPath() {
        return directory;
    }

    @Override
    public String getName() {
        return name;
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
        if (Util.isNull(name)) throw new NullPointerException();
        return getSubServers().get(name.toLowerCase());
    }

    @Override
    public SubServer addSubServer(UUID player, String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        if (plugin.api.getServers().keySet().contains(name.toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        SubServer server = new ExternalSubServer(this, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        SubAddServerEvent event = new SubAddServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            queue(new PacketExAddServer(name, enabled, port, log, directory, executable, stopcmd, (server.isRunning())?((ExternalSubLogger) server.getLogger()).getExternalAddress():null));
            servers.put(name.toLowerCase(), server);
            return server;
        } else {
            return null;
        }
    }

    @Override
    public boolean removeSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (getSubServer(server).isRunning()) {
                getSubServer(server).stop();
                getSubServer(server).waitFor();
            }
            queue(new PacketExRemoveServer(server, data -> {
                if (data.getInt("r") == 0) {
                    servers.remove(server.toLowerCase());
                }
            }));
            return true;
        } else return false;
    }

    @Override
    public boolean forceRemoveSubServer(UUID player, String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (getSubServer(server).isRunning()) {
            getSubServer(server).terminate();
        }
        queue(new PacketExRemoveServer(server, data -> {
            if (data.getInt("r") == 0) {
                servers.remove(server.toLowerCase());
            }
        }));
        return true;
    }

    @Override
    public boolean deleteSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (getSubServer(server).isRunning()) {
                getSubServer(server).stop();
                getSubServer(server).waitFor();
            }

            System.out.println("SubServers > Saving...");
            YAMLSection info = (plugin.config.get().getSection("Servers").getKeys().contains(server))?plugin.config.get().getSection("Servers").getSection(server).clone():new YAMLSection();
            info.set("Name", server);
            info.set("Timestamp", Calendar.getInstance().getTime().getTime());
            try {
                if (plugin.config.get().getSection("Servers").getKeys().contains(server)) {
                    plugin.config.get().getSection("Servers").remove(server);
                    plugin.config.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("SubServers > Removing Files...");
            queue(new PacketExDeleteServer(server, info, data -> {
                if (data.getInt("r") == 0) {
                    servers.remove(server.toLowerCase());
                    System.out.println("SubServers > Deleted SubServer: " + server);
                } else {
                    System.out.println("SubServers > Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
                }
            }));
            return true;
        } else return false;
    }

    @Override
    public boolean forceDeleteSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (getSubServer(server).isRunning()) {
            getSubServer(server).terminate();
        }

        System.out.println("SubServers > Saving...");
        YAMLSection info = (plugin.config.get().getSection("Servers").getKeys().contains(server))?plugin.config.get().getSection("Servers").getSection(server).clone():new YAMLSection();
        info.set("Name", server);
        info.set("Timestamp", Calendar.getInstance().getTime().getTime());
        try {
            if (plugin.config.get().getSection("Servers").getKeys().contains(server)) {
                plugin.config.get().getSection("Servers").remove(server);
                plugin.config.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("SubServers > Removing Files...");
        queue(new PacketExDeleteServer(server, info, data -> {
            if (data.getInt("r") == 0) {
                for (String group : getSubServer(server).getGroups()) getSubServer(server).removeGroup(group);
                servers.remove(server.toLowerCase());
                System.out.println("SubServers > Deleted SubServer: " + server);
            } else {
                System.out.println("SubServers > Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
            }
        }));
        return true;
    }
}
