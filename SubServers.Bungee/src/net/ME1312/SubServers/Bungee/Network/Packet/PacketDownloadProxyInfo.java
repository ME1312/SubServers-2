package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private String[] proxies;
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
     * @param proxies Proxies (or null for all)
     * @param tracker Receiver ID
     */
    public PacketDownloadProxyInfo(SubProxy plugin, List<String> proxies, UUID tracker) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
        this.tracker = tracker;

        if (proxies != null) {
            this.proxies = new String[proxies.size()];
            for (int i = 0; i < this.proxies.length; ++i) this.proxies[i] = proxies.get(i).toLowerCase();
            Arrays.sort(this.proxies);
        }
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);

        ObjectMap<String> proxies = new ObjectMap<String>();
        for (Proxy proxy : plugin.api.getProxies().values()) {
            if (this.proxies == null || Arrays.binarySearch(this.proxies, proxy.getName().toLowerCase()) >= 0) {
                proxies.set(proxy.getName(), proxy.forSubData());
            }
        }
        data.set(0x0001, proxies);
        if (this.proxies != null && plugin.api.getMasterProxy() != null && (this.proxies.length <= 0 || Arrays.binarySearch(this.proxies, plugin.api.getMasterProxy().getName().toLowerCase()) >= 0)) {
            data.set(0x0002, plugin.api.getMasterProxy().forSubData());
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadProxyInfo(plugin, (data.contains(0x0001))?data.getRawStringList(0x0001):null, (data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
