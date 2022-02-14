package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Add Server Packet
 */
public class PacketAddServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String name;
    private ObjectMap<String> opt;
    private boolean subserver;
    private UUID tracker;

    /**
     * New PacketAddServer (In)
     */
    public PacketAddServer() {}

    /**
     * New PacketCreateServer [Server] (Out)
     *
     * @param player Player who added
     * @param name Name of the Server
     * @param ip IP of the Server
     * @param port Port of the Server
     * @param motd MOTD of the Server
     * @param hidden If the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketAddServer(UUID player, String name, InetAddress ip, int port, String motd, boolean hidden, boolean restricted, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(name, ip, port, motd, hidden, restricted);
        this.player = player;
        this.name = name;
        this.subserver = false;

        ObjectMap<String> opt = new ObjectMap<String>();
        opt.set("address", ip.getHostAddress() + ':' + port);
        opt.set("motd", motd);
        opt.set("restricted", restricted);
        opt.set("hidden", hidden);
        this.opt = opt;

        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    /**
     * New PacketAddServer [SubServer] (Out)
     *
     * @param player Player who Added
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param host Host of Server
     * @param port Port Number
     * @param motd Motd of the Server
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable String
     * @param stopcmd Command to Stop the Server
     * @param hidden if the server should be hidden from players
     * @param restricted Players will need a permission to join if true
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketAddServer(UUID player, String name, boolean enabled, String host, int port, String motd, boolean log, String directory, String executable, String stopcmd, boolean hidden, boolean restricted, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(host, name, enabled, port, motd, log, directory, executable, stopcmd, hidden, restricted);
        this.player = player;
        this.name = name;
        this.subserver = true;

        ObjectMap<String> opt = new ObjectMap<String>();
        opt.set("enabled", enabled);
        opt.set("host", host);
        opt.set("port", port);
        opt.set("motd", motd);
        opt.set("log", log);
        opt.set("dir", directory);
        opt.set("exec", executable);
        opt.set("stop-cmd", stopcmd);
        opt.set("restricted", restricted);
        opt.set("hidden", hidden);
        this.opt = opt;

        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);

    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, name);
        data.set(0x0002, subserver);
        data.set(0x0003, opt);
        if (player != null) data.set(0x0004, player);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(data);
    }
}
