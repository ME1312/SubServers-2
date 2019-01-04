package net.ME1312.SubServers.Bungee.Host.Internal;

import com.dosse.upnp.UPnP;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.util.*;

/**
 * Internal Host Class
 */
public class InternalHost extends Host {
    public static final boolean DRM_ALLOW = System.getProperty("RM.subservers", "true").equalsIgnoreCase("true");
    private HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    private String name;
    private boolean enabled;
    private InetAddress address;
    private SubCreator creator;
    private String directory;
    protected NamedContainer<Integer, Integer> range;
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
    public InternalHost(SubPlugin plugin, String name, Boolean enabled, InetAddress address, String directory, NamedContainer<Integer, Integer> range, String gitBash) {
        super(plugin, name, enabled, address, directory, range, gitBash);
        if (!DRM_ALLOW) throw new IllegalStateException("SubServers' hosting capabilities have been disabled by your provider");
        if (Util.isNull(plugin, name, enabled, address, directory, gitBash)) throw new NullPointerException();
        this.plugin = plugin;
        this.name = name;
        this.enabled = enabled;
        this.address = address;
        this.creator = new InternalSubCreator(this, gitBash);
        this.directory = directory;
        this.range = range;
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
        SubServer server = new InternalSubServer(this, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        SubAddServerEvent event = new SubAddServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            servers.put(name.toLowerCase(), server);
            if (UPnP.isUPnPAvailable() && plugin.config.get().getSection("Settings").getSection("UPnP", new YAMLSection()).getBoolean("Forward-Servers", false)) UPnP.openPortTCP(port);
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
            if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(getSubServer(server).getAddress().getPort()))
                UPnP.closePortTCP(getSubServer(server).getAddress().getPort());
            servers.remove(server.toLowerCase());
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
        if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(getSubServer(server).getAddress().getPort()))
            UPnP.closePortTCP(getSubServer(server).getAddress().getPort());
        servers.remove(server.toLowerCase());
        return true;
    }

    @Override
    public boolean deleteSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();
        File from = new File(getPath(), servers.get(server.toLowerCase()).getPath());
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
                        Util.copyDirectory(from, to);
                        Util.deleteDirectory(from);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                    if (!to.exists()) to.mkdirs();
                    FileWriter writer = new FileWriter(new File(to, "info.json"));
                    writer.write(info.toJSON());
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("SubServers > Deleted SubServer: " + server);
            }).start();
            return true;
        } else return false;
    }

    @Override
    public boolean forceDeleteSubServer(UUID player, String name) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();
        File from = new File(getPath(), servers.get(server.toLowerCase()).getPath());
        if (forceRemoveSubServer(player, server)) {
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
                        Util.copyDirectory(from, to);
                        Util.deleteDirectory(from);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                    if (!to.exists()) to.mkdirs();
                    FileWriter writer = new FileWriter(new File(to, "info.json"), false);
                    writer.write(info.toJSON());
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("SubServers > Deleted SubServer: " + server);
            }).start();
            return true;
        } else return false;
    }
}
