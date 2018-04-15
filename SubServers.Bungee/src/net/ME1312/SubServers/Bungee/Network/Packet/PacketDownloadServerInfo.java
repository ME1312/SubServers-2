package net.ME1312.SubServers.Bungee.Network.Packet;

import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
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
    private Server server;
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
     * @param server Server
     * @param id Receiver ID
     */
    public PacketDownloadServerInfo(SubPlugin plugin, Server server, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.server = server;
        this.id = id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("id", id);
        json.set("type", (server == null)?"invalid":((server instanceof SubServer)?"subserver":"server"));
        YAMLSection info = new YAMLSection();

        if (server != null) {
            info = new YAMLSection(new Gson().fromJson(server.toString(), Map.class));
            info.remove("type");
        }

        json.set("server", info);
        return json;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadServerInfo(plugin, plugin.api.getServer(data.getRawString("server")), (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
