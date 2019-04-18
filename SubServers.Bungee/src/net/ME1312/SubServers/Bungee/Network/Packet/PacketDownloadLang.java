package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Download Lang Packet
 */
public class PacketDownloadLang implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private UUID tracker;

    /**
     * New PacketDownloadLang (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadLang(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadLang (Out)
     *
     * @param plugin SubPlugin
     * @param tracker Receiver ID
     */
    public PacketDownloadLang(SubPlugin plugin, UUID tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        LinkedHashMap<String, Map<String, String>> full = new LinkedHashMap<>();
        for (String channel : plugin.api.getLangChannels())
            full.put(channel, plugin.api.getLang(channel));
        data.set(0x0001, full);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        client.sendPacket(new PacketDownloadLang(plugin, (data != null && data.contains(0x0000))?data.getUUID(0x0000):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
