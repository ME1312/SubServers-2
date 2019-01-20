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
 * Stop Server Packet
 */
public class PacketStopServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private UUID player;
    private boolean force;
    private String server;
    private String id;

    /**
     * New PacketStopServer (In)
     */
    public PacketStopServer() {}

    /**
     * New PacketStopServer (Out)
     *
     * @param player Player Starting
     * @param server Server
     * @param force Force Stop
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketStopServer(UUID player, String server, boolean force, Callback<YAMLSection>... callback) {
        if (Util.isNull(server, force, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.force = force;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        if (player != null) data.set("player", player.toString());
        data.set("server", server);
        data.set("force", force);
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
