package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubCreator;
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
public class PacketExRemoveServer implements PacketIn, PacketOut {
    private SubServers host;
    private int response;
    private String message;
    private String id;
    private Logger log = null;

    /**
     * New PacketExRemoveServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExRemoveServer(SubServers host) {
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
     * New PacketExRemoveServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketExRemoveServer(int response, String message, String id) {
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
            if (!host.servers.keySet().contains(data.getString("server").toLowerCase())) {
                host.subdata.sendPacket(new PacketExRemoveServer(0, "Server Didn't Exist", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (host.servers.get(data.getString("server").toLowerCase()).isRunning()) {
                host.subdata.sendPacket(new PacketExRemoveServer(2, "That server is still running.", (data.keySet().contains("id"))?data.getString("id"):null));
            } else {
                host.servers.remove(data.getString("server").toLowerCase());
                log.info.println("Removed Server \u2014 " + data.getJSONObject("server").getString("name"));
                host.subdata.sendPacket(new PacketExRemoveServer(0, "Server Removed Successfully", (data.keySet().contains("id"))?data.getString("id"):null));
            }
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExRemoveServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id"))?data.getString("id"):null));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}