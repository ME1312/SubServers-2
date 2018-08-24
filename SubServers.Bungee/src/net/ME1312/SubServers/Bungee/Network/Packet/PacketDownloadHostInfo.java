package net.ME1312.SubServers.Bungee.Network.Packet;

import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String host;
    private String id;

    /**
     * New PacketDownloadHostInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadHostInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param plugin SubPlugin
     * @param host Host (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadHostInfo(SubPlugin plugin, String host, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.host = host;
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);

        YAMLSection hosts = new YAMLSection();
        for (Host host : plugin.api.getHosts().values()) {
            if (this.host == null || this.host.length() <= 0 || this.host.equalsIgnoreCase(host.getName())) {
                hosts.set(host.getName(), new YAMLSection(new Gson().fromJson(host.toString(), Map.class)));
            }
        }
        data.set("hosts", hosts);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadHostInfo(plugin, (data.contains("host"))?data.getRawString("host"):null, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.13b");
    }
}
