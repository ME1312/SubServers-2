package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Library.Callback;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.Network.PacketOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Command Server Packet
 */
public class PacketCommandServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private UUID player;
    private String server;
    private String command;
    private String id;

    /**
     * New PacketCommandServer (In)
     */
    public PacketCommandServer() {}

    /**
     * New PacketCommandServer (Out)
     *
     * @param player Player Sending
     * @param server Server to send to
     * @param command Command to send
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCommandServer(UUID player, String server, String command, Callback<YAMLSection>... callback) {
        if (Util.isNull(server, command, callback)) throw new NullPointerException();
        this.player = player;
        this.server = server;
        this.command = command;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("id", id);
        if (player != null) data.set("player", player.toString());
        data.set("server", server);
        data.set("command", command);
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
