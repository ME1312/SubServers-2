package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.*;

/**
 * Download Player Info Packet
 */
public class PacketDownloadPlayerInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<String>>[]>();
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
    public PacketDownloadPlayerInfo(Collection<String> players, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
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
    public PacketDownloadPlayerInfo(List<UUID> players, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
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
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
