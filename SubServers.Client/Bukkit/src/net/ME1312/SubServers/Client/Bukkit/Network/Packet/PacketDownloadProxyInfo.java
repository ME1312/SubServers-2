package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, NamedContainer<Boolean, Callback<ObjectMap<String>>[]>> callbacks = new HashMap<UUID, NamedContainer<Boolean, Callback<ObjectMap<String>>[]>>();
    private String proxy;
    private UUID tracker;

    /**
     * New PacketDownloadProxyInfo (In)
     */
    public PacketDownloadProxyInfo() {}

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param proxy Proxy name (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadProxyInfo(String proxy, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.proxy = proxy;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, new NamedContainer<>(proxy != null && proxy.length() <= 0, callback));
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (proxy != null) json.set(0x0001, proxy);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        boolean mode = callbacks.get(data.getUUID(0x0000)).name();
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000)).get()) {
            if (mode) {
                callback.run((data.contains(0x0002))?new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002)):null);
            } else callback.run(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        }
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
