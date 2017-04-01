package net.ME1312.SubServers.Host.Network.Packet;


import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.SubServers;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reset Packet
 */
public class PacketInReset implements PacketIn {
    private SubServers host;

    public PacketInReset(SubServers host) {
        this.host = host;
    }

    @Override
    public void execute(JSONObject data) {
        if (data != null && data.keySet().contains("m")) {
            List<String> subservers = new ArrayList<String>();
            subservers.addAll(host.servers.keySet());

            for (String server : subservers) {
                host.servers.get(server).stop();
                try {
                    host.servers.get(server).waitFor();
                } catch (Exception e) {
                    host.log.error.println(e);
                }
            }
            subservers.clear();
            host.servers.clear();

            if (host.creator.isBusy()) {
                host.creator.terminate();
                try {
                    host.creator.waitFor();
                } catch (Exception e) {
                    host.log.error.println(e);
                }
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
