package net.ME1312.SubServers.Client.Sponge.Network.Packet;

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
 * Download Group Info Packet
 */
public class PacketDownloadGroupInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<String>>[]>();
    private String group;
    private UUID tracker;

    /**
     * New PacketDownloadGroupInfo (In)
     */
    public PacketDownloadGroupInfo() {}

    /**
     * New PacketDownloadGroupInfo (Out)
     *
     * @param group Group name (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadGroupInfo(String group, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.group = group;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (group != null) json.set(0x0001, group);
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
