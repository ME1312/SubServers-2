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
 * Create Server External Host Packet
 */
public class PacketExRemoveServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String name;
    private String id;

    /**
     * New PacketExRemoveServer (In)
     */
    public PacketExRemoveServer() {}

    /**
     * New PacketExRemoveServer (Out)
     *
     * @param name Server Name
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExRemoveServer(String name, Callback<YAMLSection>... callback) {
        if (Util.isNull(name, callback)) throw new NullPointerException();
        this.name = name;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        data.set("server", name);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        for (Callback<YAMLSection> callback : callbacks.get(data.getString("id"))) callback.run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}