package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Event.SubAddServerEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.SubProxy;

import com.dosse.upnp.UPnP;
import com.google.common.collect.Range;

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
    SubProxy plugin;

    /**
     * Creates an Internal Host
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
    public InternalHost(SubProxy plugin, String name, boolean enabled, Range<Integer> ports, boolean log, InetAddress address, String directory, String gitBash) {
        super(plugin, name, enabled, ports, log, address, directory, gitBash);
        if (!DRM_ALLOW) throw new IllegalStateException("SubServers' hosting capabilities have been disabled by your provider");
        this.plugin = plugin;
        this.name = name;
        this.enabled = enabled;
        this.address = address;
        this.creator = new InternalSubCreator(this, ports, log, gitBash);
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
    public SubServer constructSubServer(String name, boolean enabled, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted) throws InvalidServerException {
        return InternalSubServer.construct(this, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
    }

    @Override
    public boolean addSubServer(UUID player, SubServer server) throws InvalidServerException {
        if (server.getHost() != this) throw new IllegalArgumentException("That Server does not belong to this Host!");
        if (plugin.api.getServers().keySet().contains(server.getName().toLowerCase())) throw new InvalidServerException("A Server already exists with this name!");
        SubAddServerEvent event = new SubAddServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            servers.put(server.getName().toLowerCase(), server);
            if (UPnP.isUPnPAvailable() && plugin.config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Servers", false)) UPnP.openPortTCP(server.getAddress().getPort());
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean removeSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        SubServer server = servers.get(name.toLowerCase());
        SubRemoveServerEvent event = new SubRemoveServerEvent(player, this, server);
        plugin.getPluginManager().callEvent(event);
        if (forced || !event.isCancelled()) {
            if (server.isRunning()) {
                server.stop();
                server.waitFor();
            }
            servers.remove(name.toLowerCase());
            if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getAddress().getPort()))
                UPnP.closePortTCP(server.getAddress().getPort());
            return true;
        } else return false;
    }

    @Override
    protected boolean recycleSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        return recycleSubServer(player, name, forced, true);
    }

    /**
     * Deletes a SubServer (will move to 'Recently Deleted')
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param forced Forces the Deletion
     * @param multithreading Uses Multithreading for I/O
     * @return Success Status
     */
    protected boolean recycleSubServer(UUID player, String name, boolean forced, boolean multithreading) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();
        File from = new File(getPath(), servers.get(server.toLowerCase()).getPath());
        if (removeSubServer(player, server, forced)) {
            Runnable method = () -> {
                UniversalFile to = new UniversalFile(plugin.dir, "SubServers:Recently Deleted:" + server.toLowerCase());
                try {
                    if (from.exists()) {
                        Logger.get("SubServers").info("Moving Files...");
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

                Logger.get("SubServers").info("Saving...");
                YAMLSection info = (plugin.servers.get().getMap("Servers").getKeys().contains(server))?new YAMLSection(plugin.servers.get().getMap("Servers").getMap(server).get()):new YAMLSection();
                info.set("Name", server);
                info.set("Timestamp", Calendar.getInstance().getTime().getTime());
                try {
                    if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                        plugin.servers.get().getMap("Servers").remove(server);
                        plugin.servers.save();
                    }
                    if (!to.exists()) to.mkdirs();
                    FileWriter writer = new FileWriter(new File(to, "info.json"), false);
                    writer.write(info.toJSON().toString());
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Logger.get("SubServers").info("Deleted SubServer: " + server);
            };

            if (multithreading) {
                new Thread(method, "SubServers.Bungee::Internal_Server_Recycler(" + name + ')').start();
            } else method.run();
            return true;
        } else return false;
    }

    @Override
    protected boolean deleteSubServer(UUID player, String name, boolean forced) throws InterruptedException {
        return deleteSubServer(player, name, forced, true);
    }

    /**
     * Deletes a SubServer
     *
     * @param player Player Deleting
     * @param name SubServer Name
     * @param forced Forces the Deletion
     * @param multithreading Uses Multithreading for I/O
     * @return Success Status
     */
    protected boolean deleteSubServer(UUID player, String name, boolean forced, boolean multithreading) throws InterruptedException {
        if (Util.isNull(name)) throw new NullPointerException();
        String server = servers.get(name.toLowerCase()).getName();
        File from = new File(getPath(), servers.get(server.toLowerCase()).getPath());
        if (removeSubServer(player, server, forced)) {
            Runnable method = () -> {
                try {
                    if (from.exists()) {
                        Logger.get("SubServers").info("Removing Files...");
                        Util.deleteDirectory(from);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Logger.get("SubServers").info("Saving...");
                try {
                    if (plugin.servers.get().getMap("Servers").getKeys().contains(server)) {
                        plugin.servers.get().getMap("Servers").remove(server);
                        plugin.servers.save();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Logger.get("SubServers").info("Deleted SubServer: " + server);
            };

            if (multithreading) {
                new Thread(method, "SubServers.Bungee::Internal_Server_Deletion(" + name + ')').start();
            } else method.run();
            return true;
        } else return false;
    }
}
