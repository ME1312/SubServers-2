package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.SubAPI;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExCreateServer implements PacketIn, PacketOut {
    private SubServers host;
    private int response;
    private String message;
    private JSONObject info;
    private String id;

    /**
     * New PacketExCreateServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExCreateServer(SubServers host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param info Creator Info
     * @param id Receiver ID
     */
    public PacketExCreateServer(int response, String message, JSONObject info, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.info = info;
        this.id = id;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("r", response);
        json.put("m", message);
        json.put("c", info);
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            host.creator.create(data.getJSONObject("creator").getString("name"), SubCreator.ServerType.valueOf(data.getJSONObject("creator").getString("type").toUpperCase()), new Version(data.getJSONObject("creator").getString("version")),
                    data.getJSONObject("creator").getInt("ram"), data.getJSONObject("creator").getInt("port"), UUID.fromString(data.getJSONObject("creator").getString("log")), (data.keySet().contains("id"))?data.getString("id"):null);
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExCreateServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), null, (data.keySet().contains("id"))?data.getString("id"):null));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}