package net.ME1312.SubServers.Client.Common.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Create Server Packet
 */
public class PacketCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String name;
    private String host;
    private String template;
    private Version version;
    private Integer port;
    private boolean waitfor;
    private UUID tracker;

    /**
     * New PacketCreateServer (In)
     */
    public PacketCreateServer() {}

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param host Host to use
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCreateServer(UUID player, String name, String host, String template, Version version, Integer port, Consumer<ObjectMap<Integer>>... callback) {
        this(player, name, host, template, version, port, false, callback);
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param host Host to use
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port
     * @param waitfor Wait until completion to send callback
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCreateServer(UUID player, String name, String host, String template, Version version, Integer port, boolean waitfor, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(name, host, template, callback);
        this.player = player;
        this.name = name;
        this.host = host;
        this.template = template;
        this.version = version;
        this.port = port;
        this.waitfor = waitfor;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, name);
        data.set(0x0002, host);
        data.set(0x0003, template);
        if (version != null) data.set(0x0004, version);
        if (port != null)   data.set(0x0005, port);
        if (player != null) data.set(0x0006, player);
        if (waitfor) data.set(0x0007, true);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.accept(data);
        callbacks.remove(data.getUUID(0x0000));
    }
}
