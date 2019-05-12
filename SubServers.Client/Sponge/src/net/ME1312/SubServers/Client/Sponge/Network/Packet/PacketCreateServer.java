package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Sponge.Graphic.UIRenderer;

import java.util.HashMap;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private static HashMap<UUID, Callback<ObjectMap<Integer>>[]> callbacks = new HashMap<UUID, Callback<ObjectMap<Integer>>[]>();
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
    public PacketCreateServer(UUID player, String name, String host, String template, Version version, Integer port, Callback<ObjectMap<Integer>>... callback) {
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
    public PacketCreateServer(UUID player, String name, String host, String template, Version version, Integer port, boolean waitfor, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(name, host, template, callback)) throw new NullPointerException();
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

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param options Creator UI Options
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCreateServer(UUID player, UIRenderer.CreatorOptions options, Callback<ObjectMap<Integer>>... callback) {
        this(player, options, false, callback);
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param player Player Creating
     * @param options Creator UI Options
     * @param waitfor Wait until completion to send callback
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketCreateServer(UUID player, UIRenderer.CreatorOptions options, boolean waitfor, Callback<ObjectMap<Integer>>... callback) {
        if (Util.isNull(options, callback)) throw new NullPointerException();
        this.player = player;
        this.name = options.getName();
        this.host = options.getHost();
        this.template = options.getTemplate();
        this.version = options.getVersion();
        this.port = options.getPort();
        this.waitfor = waitfor;
        this.tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID);
        callbacks.put(tracker, callback);

    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
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
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        for (Callback<ObjectMap<Integer>> callback : callbacks.get(data.getUUID(0x0000))) callback.run(data);
        callbacks.remove(data.getUUID(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
