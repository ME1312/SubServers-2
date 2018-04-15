package net.ME1312.SubServers.Bungee.Network.Packet;

import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

/**
 * Download Server List Packet
 */
public class PacketDownloadServerList implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String host;
    private String group;
    private String id;

    /**
     * New PacketDownloadServerList (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerList(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadServerList (Out)
     *
     * @param plugin SubPlugin
     * @param host Host (or null for all)
     * @param group Group (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadServerList(SubPlugin plugin, String host, String group, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.host = host;
        this.group = group;
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);

        YAMLSection exServers = new YAMLSection();
        for (Server server : plugin.exServers.values()) {
            exServers.set(server.getName(), new YAMLSection(new Gson().fromJson(server.toString(), Map.class)));
        }
        data.set("servers", exServers);

        if (this.host == null || !this.host.equals("")) {
            YAMLSection hosts = new YAMLSection();
            for (Host host : plugin.api.getHosts().values()) {
                if (this.host == null || this.host.equalsIgnoreCase(host.getName())) {
                    hosts.set(host.getName(), new YAMLSection(new Gson().fromJson(host.toString(), Map.class)));
                }
            }
            data.set("hosts", hosts);
        }

        if (this.group == null || !this.group.equals("")) {
            YAMLSection groups = new YAMLSection();
            for (String group : plugin.api.getGroups().keySet()) {
                if (this.group == null || this.group.equalsIgnoreCase(group)) {
                    YAMLSection servers = new YAMLSection();
                    for (Server server : plugin.api.getGroup(group)) {
                        servers.set(server.getName(), new YAMLSection(new Gson().fromJson(server.toString(), Map.class)));
                    }
                    groups.set(group, servers);
                }
            }
            data.set("groups", groups);
        }
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadServerList(plugin, (data.contains("host"))?data.getRawString("host"):null, (data.contains("group"))?data.getRawString("group"):null, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
