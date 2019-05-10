package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.Host.ServerContainer;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Link External Host Packet
 */
public class PacketLinkExHost implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubPlugin plugin;
    private int response;

    /**
     * New PacketLinkExHost (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkExHost(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkExHost (Out)
     *
     * @param response Response ID
     */
    public PacketLinkExHost(int response) {
        this.response = response;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0001, response);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            Map<String, Host> hosts = plugin.api.getHosts();
            if (hosts.keySet().contains(data.getRawString(0x0000).toLowerCase())) {
                Host host = hosts.get(data.getRawString(0x0000).toLowerCase());
                if (host instanceof ExternalHost) {
                    Integer channel = data.getInt(0x0001);
                    HashMap<Integer, SubDataClient> subdata = Util.getDespiteException(() -> Util.reflect(ExternalHost.class.getDeclaredField("subdata"), host), null);
                    if (!subdata.keySet().contains(channel) || (channel == 0 && subdata.get(0) == null)) {
                        ((ExternalHost) host).setSubData(client, channel);
                        System.out.println("SubData > " + client.getAddress().toString() + " has been defined as Host: " + host.getName() + ((channel > 0)?" (Sub "+channel+")":""));
                        client.sendPacket(new PacketLinkExHost(0));
                    } else {
                        client.sendPacket(new PacketLinkExHost(3));
                    }
                } else {
                    client.sendPacket(new PacketLinkExHost(4));
                }
            } else {
                client.sendPacket(new PacketLinkExHost(2));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketLinkExHost(1));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
