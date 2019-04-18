package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server External Host Packet
 */
public class PacketExCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;
    private UUID log;
    private UUID tracker = null;

    /**
     * New PacketExCreateServer
     */
    public PacketExCreateServer(String name) {
        this.name = name;
    }

    /**
     * New PacketExCreateServer (Out)
     *
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     * @param log Log Address
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExCreateServer(String name, SubCreator.ServerTemplate template, Version version, int port, UUID log, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(name, template, version, port, log, callback)) throw new NullPointerException();
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
        this.log = log;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker == null) {
            data.set(0x0001, name);
        } else {
            data.set(0x0000, tracker);
            data.set(0x0002, name);
            data.set(0x0003, template.getName());
            data.set(0x0004, version);
            data.set(0x0005, port);
            data.set(0x0006, log.toString());
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}