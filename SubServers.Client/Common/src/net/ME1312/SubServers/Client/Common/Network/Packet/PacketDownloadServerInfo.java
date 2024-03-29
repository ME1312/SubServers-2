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
 * Download Server Info Packet
 */
public class PacketDownloadServerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<String>>[]>();
    private List<String> servers;
    private UUID tracker;

    /**
     * New PacketDownloadServerInfo (In)
     */
    public PacketDownloadServerInfo() {}

    /**
     * New PacketDownloadServerInfo (Out)
     *
     * @param servers Server names (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadServerInfo(List<String> servers, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        this.servers = servers;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (servers != null) json.set(0x0001, servers);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<String>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
    }
}
