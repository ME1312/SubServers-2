package net.ME1312.SubServers.Host.Network.Packet;

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
 * Download Proxy Info Packet
 */
public class PacketDownloadProxyInfo implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<String>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<String>>[]>();
    private List<String> proxies;
    private UUID tracker;

    /**
     * New PacketDownloadProxyInfo (In)
     */
    public PacketDownloadProxyInfo() {}

    /**
     * New PacketDownloadProxyInfo (Out)
     *
     * @param proxies Proxies name (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadProxyInfo(List<String> proxies, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.proxies = proxies;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, tracker);
        if (proxies != null) json.set(0x0001, proxies);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getUUID(0x0000))) {
            ObjectMap<String> map = new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001));
            ObjectMap<String> master = (data.contains(0x0002))?new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002)):null;

            if (master != null) map.set(master.getString("name"), master);
            callback.run(map);
        }
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
