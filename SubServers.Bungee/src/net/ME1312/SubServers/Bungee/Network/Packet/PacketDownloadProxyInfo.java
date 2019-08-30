package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.UUID;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String proxy;
    private UUID tracker;

    /**
     * New PacketDownloadProxyInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadProxyInfo(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param plugin SubPlugin
     * @param proxy Proxy (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadProxyInfo(SubProxy plugin, String proxy, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.proxy = proxy;
        this.tracker = tracker;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> proxies = new ObjectMap<String>();
        for (Proxy proxy : plugin.api.getProxies().values()) {
            if (this.proxy == null || this.proxy.equalsIgnoreCase(proxy.getName())) {
                proxies.set(proxy.getName(), proxy.forSubData());
            }
        }
        data.set(0x0001, proxies);
        if ((this.proxy == null || this.proxy.length() <= 0) && plugin.api.getMasterProxy() != null) data.set(0x0002, plugin.api.getMasterProxy().forSubData());
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadProxyInfo(plugin, (data.contains(0x0001))?data.getRawString(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
