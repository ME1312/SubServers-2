package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.ME1312.SubServers.Sync.Network.PacketOut;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.ME1312.SubServers.Sync.Server.Server;
import net.ME1312.SubServers.Sync.Server.SubServer;
import net.ME1312.SubServers.Sync.SubPlugin;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

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
                    Method m = SubDataClient.class.getDeclaredMethod("init");
                    m.setAccessible(true);
                    m.invoke(plugin.subdata);
                    m.setAccessible(false);
                } catch (Exception e) {}
            } else {
                System.out.println("SubServers > Could not authorize SubData connection: " + data.getString("m"));
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
