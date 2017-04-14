package net.ME1312.SubServers.Bungee.Host.Internal;

import com.google.common.io.Files;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.util.*;

/**
 * Internal Host Class
 */
public class InternalHost extends Host {
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private String name;
    private boolean enabled;
    private InetAddress address;
    private SubCreator creator;
    private String directory;
    protected SubPlugin plugin;

    /**
     * Creates an Internal Host
     *
     * @param plugin Plugin
     * @param name Name
     * @param enabled Enabled Status
     * @param address Address
     * @param directory Directory
     * @param gitBash Git Bash Location
     */
    public InternalHost(SubPlugin plugin, String name, Boolean enabled, InetAddress address, String directory, String gitBash) {
        super(plugin, name, enabled, address, directory, gitBash);
        if (Util.isNull(plugin, name, enabled, address, directory, gitBash)) throw new NullPointerException();
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
        if (Util.isNull(name)) throw new NullPointerException();
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
        if (Util.isNull(name)) throw new NullPointerException();
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

    @Override
    public boolean deleteSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();
        File from = new File(getDirectory(), servers.get(server.toLowerCase()).getDirectory());
        if (removeSubServer(player, server)) {
            new Thread(() -> {
                UniversalFile to = new UniversalFile(plugin.dir, "SubServers:Recently Deleted:" + server.toLowerCase());
                try {
                    if (from.exists()) {
                        System.out.println("SubServers > Removing Files...");
                        if (to.exists()) {
                            if (to.isDirectory()) Util.deleteDirectory(to);
                            else to.delete();
                        }
                        to.mkdirs();
                        Files.move(from, to);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("SubServers > Saving...");
                JSONObject json = (plugin.config.get().getSection("Servers").getKeys().contains(server))?plugin.config.get().getSection("Servers").getSection(server).toJSON():new JSONObject();
                json.put("Name", server);
                json.put("Timestamp", Calendar.getInstance().getTime().getTime());
                try {
                    if (plugin.config.get().getSection("Servers").getKeys().contains(server)) {
                        plugin.config.get().getSection("Servers").remove(server);
                        plugin.config.save();
                    }
                    if (!to.exists()) to.mkdirs();
                    FileWriter writer = new FileWriter(new File(to, "info.json"));
                    json.write(writer);
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("SubServers > Done!");
            }).start();
            return true;
        } else return false;
    }
}
