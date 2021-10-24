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
 * Download Group Info Packet
 */
public class PacketDownloadGroupInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<String>>[]>();
    private List<String> groups;
    private UUID tracker;

    /**
     * New PacketDownloadGroupInfo (In)
     */
    public PacketDownloadGroupInfo() {}

    /**
     * New PacketDownloadGroupInfo (Out)
     *
     * @param groups Group names (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadGroupInfo(List<String> groups, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        this.groups = groups;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (groups != null) json.set(0x0001, groups);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) {
            ObjectMap<String> map = new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001));
            ObjectMap<String> ungrouped = (data.contains(0x0002))?new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002)):null;

            if (ungrouped != null) map.set("", ungrouped);
            callback.accept(map);
        }
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
