package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Create Server External Host Packet
 */
public class PacketExCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Consumer<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Consumer<ObjectMap<Integer>>[]>();
    private UUID player;
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;
    private Boolean mode;
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
     * @param player Player
     * @param server Server to Update
     * @param template Server Template
     * @param version Server Version
     * @param log Log Address
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExCreateServer(UUID player, SubServer server, SubCreator.ServerTemplate template, Version version, UUID log, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(server, template, log, callback);
        this.player = player;
        this.name = server.getName();
        this.template = template;
        this.version = version;
        this.port = server.getAddress().getPort();
        this.mode = template == server.getTemplate();
        this.log = log;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);
    }

    /**
     * New PacketExCreateServer (Out)
     *
     * @param player Player
     * @param name Server Name
     * @param template Server Template
     * @param version Server Version
     * @param port Server Port Number
     * @param log Log Address
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketExCreateServer(UUID player, String name, SubCreator.ServerTemplate template, Version version, int port, UUID log, Consumer<ObjectMap<Integer>>... callback) {
        Util.nullpo(name, template, port, log, callback);
        this.player = player;
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
        this.mode = null;
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
            if (template.isDynamic()) {
                ObjectMap<String> template = new ObjectMap<>();
                template.set("name", this.template.getName());
                template.set("display", this.template.getDisplayName());
                template.set("enabled", this.template.isEnabled());
                template.set("icon", this.template.getIcon());
                template.set("dir", this.template.getDirectory().toString());
                template.set("build", this.template.getBuildOptions().clone());
                template.set("def", this.template.getConfigOptions().clone());
                data.set(0x0003, template);
            } else {
                data.set(0x0003, template.getName());
            }
            data.set(0x0004, version);
            data.set(0x0005, port);
            data.set(0x0006, log);
            if (mode != null)
                data.set(0x0007, mode);
            if (player != null)
                data.set(0x0008, player);
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Consumer<ObjectMap<Integer>> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept(data);
    }
}