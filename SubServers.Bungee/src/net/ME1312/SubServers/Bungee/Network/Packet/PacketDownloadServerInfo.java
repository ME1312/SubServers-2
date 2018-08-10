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
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String server;
    private String id;

    /**
     * New PacketDownloadServerInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadServerInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param plugin SubPlugin
     * @param server Server (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadServerInfo(SubPlugin plugin, String server, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.server = server;
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);

        YAMLSection servers = new YAMLSection();
        for (Server server : plugin.api.getServers().values()) {
            if (this.server == null || this.server.length() <= 0 || this.server.equalsIgnoreCase(server.getName())) {
                servers.set(server.getName(), new YAMLSection(new Gson().fromJson(server.toString(), Map.class)));
            }
        }
        data.set("servers", servers);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadServerInfo(plugin, (data.contains("server"))?data.getRawString("server"):null, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.13b");
    }
}
