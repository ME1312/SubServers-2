package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.SubCreator;
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
public class PacketExCreateServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String name;
    private SubCreator.ServerTemplate template;
    private Version version;
    private int port;
    private UUID log;
    private String id = null;

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
    public PacketExCreateServer(String name, SubCreator.ServerTemplate template, Version version, int port, UUID log, Callback<YAMLSection>... callback) {
        if (Util.isNull(name, template, version, port, log, callback)) throw new NullPointerException();
        this.name = name;
        this.template = template;
        this.version = version;
        this.port = port;
        this.log = log;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        if (id == null) {
            YAMLSection data = new YAMLSection();
            data.set("thread", name);
            return data;
        } else {
            YAMLSection data = new YAMLSection();
            data.set("id", id);
            YAMLSection creator = new YAMLSection();
            creator.set("name", name);
            creator.set("template", template.getName());
            creator.set("version", version);
            creator.set("port", port);
            creator.set("log", log.toString());
            data.set("creator", creator);
            return data;
        }
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        for (Callback<YAMLSection> callback : callbacks.get(data.getRawString("id"))) callback.run(data);
        callbacks.remove(data.getRawString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}