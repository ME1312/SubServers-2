package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Callback;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.Network.PacketOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Download Server List Packet
 */
public class PacketDownloadServerList implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String host;
    private String group;
    private String id;

    /**
     * New PacketDownloadServerList (In)
     */
    public PacketDownloadServerList() {}

    /**
     * New PacketDownloadServerList (Out)
     *
     * @param host Host name (or null for all)
     * @param group Group name (or null for all)
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadServerList(String host, String group, Callback<YAMLSection>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.host = host;
        this.group = group;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("id", id);
        if (host != null) json.set("host", host);
        if (group != null) json.set("group", group);
        return json;
    }

    @Override
    public void execute(YAMLSection data) {
        for (Callback<YAMLSection> callback : callbacks.get(data.getRawString("id"))) callback.run(data);
        callbacks.remove(data.getRawString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
