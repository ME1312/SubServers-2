package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Callback;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Delete Server External Host Packet
 */
public class PacketExDeleteServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String name;
    private YAMLSection info;
    private String id = null;

    /**
     * New PacketExDeleteServer
     */
    public PacketExDeleteServer() {}

    /**
     * New PacketExDeleteServer (Out)
     *
     * @param name Server Name
     * @param info Info.json Contents
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExDeleteServer(String name, YAMLSection info, Callback<YAMLSection>... callback) {
        if (Util.isNull(name, info, callback)) throw new NullPointerException();
        this.name = name;
        this.info = info;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        if (id == null) {
            return null;
        } else {
            YAMLSection data = new YAMLSection();
            data.set("id", id);
            data.set("server", name);
            data.set("info", info);
            return data;
        }
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        for (Callback<YAMLSection> callback : callbacks.get(data.getRawString("id"))) callback.run(data);
        callbacks.remove(data.getRawString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}