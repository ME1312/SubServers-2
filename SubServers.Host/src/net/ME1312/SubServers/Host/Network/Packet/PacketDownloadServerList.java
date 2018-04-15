package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Callback;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;

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
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        if (host != null) data.set("host", host);
        if (group != null) data.set("group", group);
        return data;
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
