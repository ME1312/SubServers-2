package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<String>>[]>();
    private List<String> hosts;
    private UUID tracker;

    /**
     * New PacketDownloadHostInfo (In)
     */
    public PacketDownloadHostInfo() {}

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param hosts Host names (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadHostInfo(List<String> hosts, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.hosts = hosts;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (hosts != null) json.set(0x0001, hosts);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
