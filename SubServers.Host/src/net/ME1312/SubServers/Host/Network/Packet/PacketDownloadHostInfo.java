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
 * Download Host Info Packet
 */
public class PacketDownloadHostInfo implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String host;
    private String id;

    /**
     * New PacketDownloadHostInfo (In)
     */
    public PacketDownloadHostInfo() {}

    /**
     * New PacketDownloadHostInfo (Out)
     *
     * @param host Host Name
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadHostInfo(String host, Callback<YAMLSection>... callback) {
        if (Util.isNull(host, callback)) throw new NullPointerException();
        this.host = host;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        data.set("host", host);
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
