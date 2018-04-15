package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Callback;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Download Player List Packet
 */
public class PacketDownloadPlayerList implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String id;

    /**
     * New PacketDownloadPlayerList (In)
     */
    public PacketDownloadPlayerList() {}

    /**
     * New PacketDownloadPlayerList (Out)
     *
     * @param callback Callbacks
     */
    public PacketDownloadPlayerList(Callback... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }
    @Override
    public YAMLSection generate() {
        if (id != null) {
            YAMLSection json = new YAMLSection();
            json.set("id", id);
            return json;
        } else {
            return null;
        }
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
