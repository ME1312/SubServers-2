package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    /**
     * New PacketDownloadProxyInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadProxyInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param plugin SubPlugin
     * @param id Receiver ID
     */
    public PacketDownloadProxyInfo(SubPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);
        YAMLSection subservers = new YAMLSection();
        subservers.set("version", plugin.version.toString());
        subservers.set("last-reload", plugin.resetDate);
        subservers.set("hosts", plugin.api.getHosts().size());
        subservers.set("subservers", plugin.api.getSubServers().size());
        data.set("subservers", subservers);
        YAMLSection bungee = new YAMLSection();
        bungee.set("version", plugin.api.getProxyVersion());
        bungee.set("servers", plugin.api.getServers().size());
        data.set("bungee", bungee);
        YAMLSection minecraft = new YAMLSection();
        minecraft.set("version", plugin.api.getGameVersion());
        minecraft.set("players", plugin.api.getGlobalPlayers().size());
        data.set("minecraft", minecraft);
        YAMLSection system = new YAMLSection();
        YAMLSection os = new YAMLSection();
        os.set("name", System.getProperty("os.name"));
        os.set("version", System.getProperty("os.version"));
        system.set("os", os);
        YAMLSection java = new YAMLSection();
        java.set("version",  System.getProperty("java.version"));
        system.set("java", java);
        data.set("system", system);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadProxyInfo(plugin, (data != null && data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
