package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadPlatformInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<String>>[]>();
    private UUID tracker;
    /**
     * New PacketDownloadPlatformInfo
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadPlatformInfo(Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<String>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
    }
}
