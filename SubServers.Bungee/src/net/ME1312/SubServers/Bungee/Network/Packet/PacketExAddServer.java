package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Callback;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;

import java.util.HashMap;
import java.util.UUID;

/**
 * Add Server External Host Packet
 */
public class PacketExAddServer implements PacketIn, PacketOut {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String name;
    private boolean enabled;
    private int port;
    private boolean log;
    private String directory;
    private Executable executable;
    private String stopcmd;
    private UUID running;
    private String id;

    /**
     * New PacketExAddServer (In)
     */
    public PacketExAddServer() {}

    /**
     * New PacketExAddServer (Out)
     *
     * @param name Name of Server
     * @param enabled Enabled Status
     * @param log Logging Status
     * @param directory Directory
     * @param executable Executable
     */
    @SafeVarargs
    public PacketExAddServer(String name, boolean enabled, int port, boolean log, String directory, Executable executable, String stopcmd, UUID running, Callback<YAMLSection>... callback) {
        if (Util.isNull(name, enabled, log, directory, executable, callback)) throw new NullPointerException();
        this.name = name;
        this.enabled = enabled;
        this.port = port;
        this.log = log;
        this.directory = directory;
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.running = running;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);
        YAMLSection server = new YAMLSection();
        server.set("name", name);
        server.set("enabled", enabled);
        server.set("port", port);
        server.set("log", log);
        server.set("dir", directory);
        server.set("exec", executable.toString());
        server.set("stopcmd", stopcmd);
        if (running != null) server.set("running", running.toString());
        data.set("server", server);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        for (Callback<YAMLSection> callback : callbacks.get(data.getRawString("id"))) callback.run(data);
        callbacks.remove(data.getRawString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.13.1b");
    }
}