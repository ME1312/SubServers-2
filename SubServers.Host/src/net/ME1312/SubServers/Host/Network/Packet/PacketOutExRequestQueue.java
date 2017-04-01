package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * Queue Request Packet
 */
public class PacketOutExRequestQueue implements PacketOut {

    /**
     * New PacketOutExRequestQueue
     */
    public PacketOutExRequestQueue() {
    }

    @Override
    public JSONObject generate() {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
