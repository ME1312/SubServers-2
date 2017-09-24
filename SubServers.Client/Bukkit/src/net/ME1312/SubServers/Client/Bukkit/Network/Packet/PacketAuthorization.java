package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;

public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;

    public PacketAuthorization(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("password", plugin.config.get().getSection("Settings").getSection("SubData").getString("Password"));
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (data.getInt("r") == 0) {
                try {
                    Method m = SubDataClient.class.getDeclaredMethod("sendPacket", NamedContainer.class);
                    m.setAccessible(true);
                    m.invoke(plugin.subdata, new NamedContainer<String, PacketOut>(null, new PacketLinkServer(plugin)));
                    m.setAccessible(false);
                } catch (Exception e) {}
            } else {
                Bukkit.getLogger().info("SubServers > Could not authorize SubData connection: " + data.getString("m"));
                plugin.subdata.destroy(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
