package net.ME1312.SubServers.Bungee.Network.Packet;

import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Proxy;
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
 * Download Group Info Packet
 */
public class PacketDownloadGroupInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String host;
    private String group;
    private String id;

    /**
     * New PacketDownloadGroupInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadGroupInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadGroupInfo (Out)
     *
     * @param plugin SubPlugin
     * @param group Group (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadGroupInfo(SubPlugin plugin, String group, String id) {
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

        YAMLSection groups = new YAMLSection();
        for (String group : plugin.api.getGroups().keySet()) {
            if (this.group == null || this.group.length() <= 0 || this.group.equalsIgnoreCase(group)) {
                YAMLSection servers = new YAMLSection();
                for (Server server : plugin.api.getGroup(group)) {
                    servers.set(server.getName(), new YAMLSection(new Gson().fromJson(server.toString(), Map.class)));
                }
                groups.set(group, servers);
            }
        }
        data.set("groups", groups);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadGroupInfo(plugin, (data.contains("group"))?data.getRawString("group"):null, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.13b");
    }
}
