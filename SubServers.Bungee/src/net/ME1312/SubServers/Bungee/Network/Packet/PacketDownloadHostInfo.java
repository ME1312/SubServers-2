package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

import net.ME1312.SubServers.Bungee.SubPlugin;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private Host host;
    private String id;

    /**
     * New PacketDownloadHostInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadHostInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param plugin SubPlugin
     * @param host Host
     * @param id Receiver ID
     */
    public PacketDownloadHostInfo(SubPlugin plugin, Host host, String id) {
        this.plugin = plugin;
        this.host = host;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        YAMLSection info = new YAMLSection();

        if (host != null) {
            data.set("valid", true);
            info = new YAMLSection(host.toString());
            info.remove("type");
        } else data.set("valid", false);

        data.set("host", info);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadHostInfo(plugin, plugin.api.getHost(data.getRawString("host")), (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
