package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;

    /**
     * New PacketLinkServer
     *
     * @param plugin SubServers.Client
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("name", plugin.subdata.getName());
        json.put("port", Bukkit.getServer().getPort());
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        if (data.getInt("r") == 0) {
            try {
                if (data.keySet().contains("n")) {
                    Field f = SubDataClient.class.getDeclaredField("name");
                    f.setAccessible(true);
                    f.set(plugin.subdata, data.getString("n"));
                    f.setAccessible(false);
                }
                Method m = SubDataClient.class.getDeclaredMethod("init");
                m.setAccessible(true);
                m.invoke(plugin.subdata);
                m.setAccessible(false);
            } catch (Exception e) {}
        } else {
            try {
                if (data.getInt("r") == 2) {
                    if (!plugin.config.get().getSection("Settings").getSection("SubData").contains("Name")) {
                        plugin.config.get().getSection("Settings").getSection("SubData").set("Name", "undefined");
                        plugin.config.save();
                    }
                }
            } catch (Exception e) {}
            Bukkit.getLogger().info("SubData > Could not link name with server: " + data.getString("m"));
            plugin.onDisable();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
