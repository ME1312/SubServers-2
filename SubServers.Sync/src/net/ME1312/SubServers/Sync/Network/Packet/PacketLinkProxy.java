package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.Network.PacketOut;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.ME1312.SubServers.Sync.SubPlugin;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * Link Proxy Packet
 */
public class PacketLinkProxy implements PacketIn, PacketOut {
    private SubPlugin plugin;

    /**
     * New PacketLinkProxy
     *
     * @param plugin SubServers.Sync
     */
    public PacketLinkProxy(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("name", plugin.subdata.getName());
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        if (data.getInt("r") == 0) {
            if (data.keySet().contains("n")) try {
                Field m = SubDataClient.class.getDeclaredField("name");
                m.setAccessible(true);
                m.set(plugin.subdata, data.getString("n"));
                m.setAccessible(false);
            } catch (Exception e) {}
        } else {
            System.out.println("SubData > Could not link name with server: " + data.getString("m"));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
