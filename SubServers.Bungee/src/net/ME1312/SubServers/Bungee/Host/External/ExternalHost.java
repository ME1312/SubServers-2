package net.ME1312.SubServers.Bungee.Host.External;

import com.google.common.collect.Range;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExAddServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDeleteServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExRemoveServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutExReset;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.net.InetAddress;
import java.util.*;

/**
 * External Host Class
 */
public class ExternalHost extends Host implements ClientHandler {
    private HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private String name;
    protected boolean available;
    private boolean enabled;
    private InetAddress address;
    private SubCreator creator;
    private String directory;
    private LinkedList<PacketObjectOut> queue;
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
        this.available = false;
        this.enabled = enabled;
        this.address = address;
        this.creator = new ExternalSubCreator(this, ports, log, gitBash);
        this.directory = directory;
        this.queue = new LinkedList<PacketObjectOut>();
        this.clean = false;

        subdata.put(0, null);
    }

    @Override
    public DataClient[] getSubData() {
        LinkedList<Integer> keys = new LinkedList<Integer>(subdata.keySet());
        LinkedList<SubDataClient> channels = new LinkedList<SubDataClient>();
        Collections.sort(keys);
        for (Integer channel : keys) channels.add(subdata.get(channel));
        return channels.toArray(new DataClient[0]);
    }

    public void setSubData(DataClient client, int channel) {
        if (channel < 0) throw new IllegalArgumentException("Subchannel ID cannot be less than zero");
        if (client != null || channel == 0) {
            if (!subdata.keySet().contains(channel) || (channel == 0 && (client == null || subdata.get(channel) == null))) {
                subdata.put(channel, (SubDataClient) client);
                if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) ((SubDataClient) client).setHandler(this);
            }
        } else {
            subdata.remove(channel);
        }
    }

    @Override
    public void removeSubData(DataClient client) {
        for (Integer channel : Util.getBackwards(subdata, (SubDataClient) client)) setSubData(null, channel);
    }

    protected void queue(PacketObjectOut... packet) {
        for (PacketObjectOut p : packet) if (getSubData()[0] == null || !available) {
            queue.add(p);
        } else {
            ((SubDataClient) getSubData()[0]).sendPacket(p);
        }
    }
    private void requeue() {
        SubDataClient client = (SubDataClient) getSubData()[0];
        if (!clean) {
            client.sendPacket(new PacketOutExReset("Prevent Desync"));
            clean = true;
        }
        for (SubServer server : servers.values()) {
            client.sendPacket(new PacketExAddServer(server.getName(), server.isEnabled(), server.getAddress().getPort(), server.isLogging(), server.getPath(), ((ExternalSubServer) server).exec, server.getStopCommand(), (server.isRunning())?((ExternalSubLogger) server.getLogger()).getExternalAddress():null));
        }
        while (queue.size() != 0) {
            client.sendPacket(queue.get(0));
            queue.remove(0);
        }
        available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
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
                if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
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
            if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
                servers.remove(server.toLowerCase());
            }
        }));
        return true;
    }

    @Override
    public boolean recycleSubServer(UUID player, String name) throws InterruptedException {
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
            ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
            info.set("Name", server);
            info.set("Timestamp", Calendar.getInstance().getTime().getTime());
            try {
                if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                    plugin.servers.get().getMap("Servers").remove(server);
                    plugin.servers.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("SubServers > Moving Files...");
            queue(new PacketExDeleteServer(server, info, true, data -> {
                if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
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
    public boolean forceRecycleSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();

        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, getSubServer(server));
        plugin.getPluginManager().callEvent(event);
        if (getSubServer(server).isRunning()) {
            getSubServer(server).terminate();
        }

        System.out.println("SubServers > Saving...");
        ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
        info.set("Name", server);
        info.set("Timestamp", Calendar.getInstance().getTime().getTime());
        try {
            if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                plugin.servers.get().getMap("Servers").remove(server);
                plugin.servers.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("SubServers > Moving Files...");
        queue(new PacketExDeleteServer(server, info, true, data -> {
            if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
                for (String group : getSubServer(server).getGroups()) getSubServer(server).removeGroup(group);
                servers.remove(server.toLowerCase());
                System.out.println("SubServers > Deleted SubServer: " + server);
            } else {
                System.out.println("SubServers > Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
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
            ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
            info.set("Name", server);
            info.set("Timestamp", Calendar.getInstance().getTime().getTime());
            try {
                if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                    plugin.servers.get().getMap("Servers").remove(server);
                    plugin.servers.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("SubServers > Removing Files...");
            queue(new PacketExDeleteServer(server, info, false, data -> {
                if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
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
        ObjectMap<String> info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?plugin.servers.get().getMap("Servers").getMap(server).clone():new ObjectMap<String>();
        info.set("Name", server);
        info.set("Timestamp", Calendar.getInstance().getTime().getTime());
        try {
            if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                plugin.servers.get().getMap("Servers").remove(server);
                plugin.servers.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("SubServers > Removing Files...");
        queue(new PacketExDeleteServer(server, info, false, data -> {
            if (data.getInt(0x0001) == 0 || data.getInt(0x0001) == 1) {
                for (String group : getSubServer(server).getGroups()) getSubServer(server).removeGroup(group);
                servers.remove(server.toLowerCase());
                System.out.println("SubServers > Deleted SubServer: " + server);
            } else {
                System.out.println("SubServers > Couldn't remove " + server + " from memory. See " + getName() + " console for more details");
            }
        }));
        return true;
    }

    @Override
    public ObjectMap<String> forSubData() {
        ObjectMap<String> hinfo = super.forSubData();
        ObjectMap<Integer> subdata = new ObjectMap<Integer>();
        for (int channel : this.subdata.keySet()) subdata.set(channel, (this.subdata.get(channel) == null)?null:this.subdata.get(channel).getID());
        hinfo.set("subdata", subdata);
        return hinfo;
    }
}
