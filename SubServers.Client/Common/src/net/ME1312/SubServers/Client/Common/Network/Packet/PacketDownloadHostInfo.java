package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<String>>[]>();
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
    public PacketDownloadHostInfo(List<String> hosts, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
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
        for (Consumer<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
