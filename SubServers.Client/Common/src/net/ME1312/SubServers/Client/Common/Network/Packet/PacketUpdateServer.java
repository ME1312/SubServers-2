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
public class PacketUpdateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String name;
    private String template;
    private Version version;
    private boolean waitfor;
    private UUID tracker;

    /**
     * New PacketCreateServer (In)
     */
    public PacketUpdateServer() {}

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketUpdateServer(UUID player, String name, String template, Version version, Consumer<ObjectMap<Integer>>... callback) {
        this(player, name, template, version, false, callback);
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param waitfor Wait until completion to send callback
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketUpdateServer(UUID player, String name, String template, Version version, boolean waitfor, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(name, callback);
        this.player = player;
        this.name = name;
        this.template = template;
        this.version = version;
        this.waitfor = waitfor;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, name);
        if (template != null) data.set(0x0002, template);
        if (version != null) data.set(0x0003, version);
        if (player != null) data.set(0x0004, player);
        if (waitfor) data.set(0x0005, true);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(data);
    }
}
