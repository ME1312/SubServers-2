package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Library.JSONCallback;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

/**
 * Add Server External Host Packet
 */
public class PacketExAddServer implements PacketIn, PacketOut {
    private static HashMap<String, JSONCallback[]> callbacks = new HashMap<String, JSONCallback[]>();
    private String name;
    private boolean enabled;
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
    public PacketExAddServer(String name, boolean enabled, boolean log, String directory, Executable executable, String stopcmd, UUID running, JSONCallback... callback) {
        if (Util.isNull(name, enabled, log, directory, executable, callback)) throw new NullPointerException();
        this.name = name;
        this.enabled = enabled;
        this.log = log;
        this.directory = directory;
        this.executable = executable;
        this.stopcmd = stopcmd;
        this.running = running;
        this.id = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString();
        callbacks.put(id, callback);
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONObject server = new JSONObject();
        server.put("name", name);
        server.put("enabled", enabled);
        server.put("log", log);
        server.put("dir", directory);
        server.put("exec", executable.toString());
        server.put("stopcmd", stopcmd);
        if (running != null) server.put("running", running.toString());
        json.put("server", server);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        for (JSONCallback callback : callbacks.get(data.getString("id"))) callback.run(data);
        callbacks.remove(data.getString("id"));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}