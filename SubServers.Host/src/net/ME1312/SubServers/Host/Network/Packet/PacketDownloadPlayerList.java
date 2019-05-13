package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Download Player List Packet
 */
public class PacketDownloadPlayerList implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<String>>[]>();
    private UUID tracker;

    /**
     * New PacketDownloadPlayerList
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadPlayerList(Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
