package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.*;
import java.util.function.Consumer;

/**
 * Download Player Info Packet
 */
public class PacketDownloadPlayerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<String>>[]>();
    private Collection<String> names;
    private Collection<UUID> ids;
    private UUID tracker;

    /**
     * New PacketDownloadPlayerInfo (In)
     */
    public PacketDownloadPlayerInfo() {}

    /**
     * New PacketDownloadPlayerInfo (Out)
     *
     * @param players Player Names (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadPlayerInfo(Collection<String> players, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        this.names = players;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    /**
     * New PacketDownloadPlayerInfo (Out)
     *
     * @param players Player IDs (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadPlayerInfo(List<UUID> players, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        this.ids = players;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (names != null) json.set(0x0001, names);
        if (ids != null) json.set(0x0002, ids);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<String>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
    }
}
