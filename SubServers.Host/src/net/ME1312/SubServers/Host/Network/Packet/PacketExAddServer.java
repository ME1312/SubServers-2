package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.Executable;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExAddServer implements PacketIn, PacketOut {
    private SubServers host;
    private int response;
    private String message;
    private String id;
    private Logger log = null;

    /**
     * New PacketExAddServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExAddServer(SubServers host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    /**
     * New PacketExAddServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketExAddServer(int response, String message, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("r", response);
        json.put("m", message);
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (host.servers.keySet().contains(data.getJSONObject("server").getString("name").toLowerCase())) {
                host.subdata.sendPacket(new PacketExAddServer(0, "Server Already Added", (data.keySet().contains("id"))?data.getString("id"):null));
            } else {
                SubServer server = new SubServer(host, data.getJSONObject("server").getString("name"), data.getJSONObject("server").getBoolean("enabled"), data.getJSONObject("server").getBoolean("log"),
                        data.getJSONObject("server").getString("dir"), new Executable(data.getJSONObject("server").getString("exec")), data.getJSONObject("server").getString("stopcmd"));
                host.servers.put(data.getJSONObject("server").getString("name").toLowerCase(), server);
                log.info.println("Added Server \u2014 " + data.getJSONObject("server").getString("name"));
                if (data.getJSONObject("server").keySet().contains("running")) server.start(UUID.fromString(data.getJSONObject("server").getString("running")));
                host.subdata.sendPacket(new PacketExAddServer(0, "Server Added Successfully", (data.keySet().contains("id"))?data.getString("id"):null));
            }
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExAddServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id"))?data.getString("id"):null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}