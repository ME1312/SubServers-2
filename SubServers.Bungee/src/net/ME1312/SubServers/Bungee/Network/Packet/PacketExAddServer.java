package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubServer;

import java.util.HashMap;
import java.util.UUID;

/**
 * Add Server External Host Packet
 */
public class PacketExAddServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
    private String name;
    private boolean enabled;
    private int port;
    private boolean log;
    private String directory;
    private String executable;
    private String stopcmd;
    private UUID running;
    private UUID tracker;

    /**
     * New PacketExAddServer (In)
     */
    public PacketExAddServer() {}

    /**
     * New PacketExAddServer (Out)
     */
    @SafeVarargs
    public PacketExAddServer(ExternalSubServer server, UUID running, Callback<ObjectMap<Integer>>... callback) {
        if (callback == null) throw new NullPointerException();
        this.name = server.getName();
        this.enabled = server.isEnabled();
        this.port = server.getAddress().getPort();
        this.log = server.isLogging();
        this.directory = server.getPath();
        this.executable = server.getExecutable();
        this.stopcmd = server.getStopCommand();
        this.running = running;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, name);
        data.set(0x0002, enabled);
        data.set(0x0003, port);
        data.set(0x0004, log);
        data.set(0x0005, directory);
        data.set(0x0006, executable);
        data.set(0x0007, stopcmd);
        if (running != null) data.set(0x0008, running.toString());
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