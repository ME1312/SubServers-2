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
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String proxy;
    private String id;

    /**
     * New PacketDownloadProxyInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadProxyInfo(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param plugin SubPlugin
     * @param proxy Proxy (or null for all)
     * @param id Receiver ID
     */
    public PacketDownloadProxyInfo(SubPlugin plugin, String proxy, String id) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.proxy = proxy;
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);

        YAMLSection proxies = new YAMLSection();
        for (Proxy proxy : plugin.api.getProxies().values()) {
            if (this.proxy == null || this.proxy.equalsIgnoreCase(proxy.getName())) {
                proxies.set(proxy.getName(), new YAMLSection(new Gson().fromJson(proxy.toString(), Map.class)));
            }
        }
        data.set("proxies", proxies);
        if ((this.proxy == null || this.proxy.length() <= 0) && plugin.api.getMasterProxy() != null) data.set("master", new YAMLSection(new Gson().fromJson(plugin.api.getMasterProxy().toString(), Map.class)));
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadProxyInfo(plugin, (data.contains("proxy"))?data.getRawString("proxy"):null, (data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.13b");
    }
}
