package net.ME1312.SubServers.Client.Bukkit.Network.Packet;

import net.ME1312.SubServers.Client.Bukkit.Event.SubDataRecieveGenericInfoEvent;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketIn;
import net.ME1312.SubServers.Client.Bukkit.Network.PacketOut;
import org.bukkit.Bukkit;
import org.json.JSONObject;

public class PacketInfoPassthrough implements PacketIn, PacketOut {
    private String t;
    private String h;
    private Version v;
    private JSONObject c;

    public PacketInfoPassthrough() {}
    public PacketInfoPassthrough(String target, String handle, Version version, JSONObject content) {
        this.t = target;
        this.h = handle;
        this.v = version;
        this.c = content;
    }


    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("t", t);
        json.put("h", h);
        json.put("v", v.toString());
        json.put("c", c);
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        Bukkit.getPluginManager().callEvent(new SubDataRecieveGenericInfoEvent(data.getString("h"), new Version(data.getString("v")), data.getJSONObject("c")));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}