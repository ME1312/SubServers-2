package net.ME1312.SubServers.Proxy.Network.Packet;

import net.ME1312.SubServers.Proxy.Event.SubDataRecieveGenericInfoEvent;
import net.ME1312.SubServers.Proxy.Library.Version.Version;
import net.ME1312.SubServers.Proxy.Network.Client;
import net.ME1312.SubServers.Proxy.Network.ClientHandler;
import net.ME1312.SubServers.Proxy.Network.PacketIn;
import net.ME1312.SubServers.Proxy.Network.PacketOut;
import net.ME1312.SubServers.Proxy.SubPlugin;
import org.json.JSONObject;

import java.net.InetSocketAddress;

public class PacketInfoPassthrough implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String h;
    private Version v;
    private JSONObject c;

    public PacketInfoPassthrough(SubPlugin plugin) {
        this.plugin = plugin;
    }
    public PacketInfoPassthrough(String handle, Version version, JSONObject content) {
        this.h = handle;
        this.v = version;
        this.c = content;
    }


    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("h", h);
        json.put("v", v.toString());
        json.put("c", c);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        if (data.get("t") == null) {
            plugin.getPluginManager().callEvent(new SubDataRecieveGenericInfoEvent(data.getString("h"), new Version(data.getString("v")), data.getJSONObject("c")));
        } else {
            try {
                switch (data.getJSONObject("t").getString("type").toLowerCase()) {
                    case "address":
                        if (plugin.subdata.getClient(new InetSocketAddress(data.getJSONObject("t").getString("id").split(":")[0], Integer.parseInt(data.getJSONObject("t").getString("id").split(":")[1]))) != null)
                            plugin.subdata.getClient(new InetSocketAddress(data.getJSONObject("t").getString("id").split(":")[0], Integer.parseInt(data.getJSONObject("t").getString("id").split(":")[1])))
                                    .sendPacket(new PacketInfoPassthrough(data.getString("h"), new Version(data.getString("v")), data.getJSONObject("c")));
                        break;
                    case "host":
                        if (plugin.hosts.keySet().contains(data.getJSONObject("t").getString("id").toLowerCase()) && plugin.hosts.get(data.getJSONObject("t").getString("id").toLowerCase()) instanceof ClientHandler)
                            ((ClientHandler) plugin.hosts.get(data.getJSONObject("t").getString("id").toLowerCase())).getSubDataClient()
                                    .sendPacket(new PacketInfoPassthrough(data.getString("h"), new Version(data.getString("v")), data.getJSONObject("c")));
                        break;
                    case "server":
                    case "subserver":
                        if (plugin.api.getServers().keySet().contains(data.getJSONObject("t").getString("id").toLowerCase()))
                            plugin.api.getServers().get(data.getJSONObject("t").getString("id").toLowerCase()).getSubDataClient()
                                    .sendPacket(new PacketInfoPassthrough(data.getString("h"), new Version(data.getString("v")), data.getJSONObject("c")));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}