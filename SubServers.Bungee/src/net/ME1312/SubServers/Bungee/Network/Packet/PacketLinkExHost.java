package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.*;

import static net.ME1312.SubServers.Bungee.Network.Packet.PacketLinkServer.last;
import static net.ME1312.SubServers.Bungee.Network.Packet.PacketLinkServer.req;

/**
 * Link External Host Packet
 */
public class PacketLinkExHost implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private String message;

    /**
     * New PacketLinkExHost (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkExHost(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketLinkExHost (Out)
     *
     * @param response Response ID
     * @param message Message
     */
    public PacketLinkExHost(int response, String message) {
        Util.nullpo(response);
        this.response = response;
        this.message = message;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0001, response);
        if (message != null) data.set(0x0002, message);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        try {
            Map<String, Host> hosts = plugin.api.getHosts();
            if (hosts.keySet().contains(data.getString(0x0000).toLowerCase())) {
                Host host = hosts.get(data.getString(0x0000).toLowerCase());
                if (host instanceof ExternalHost) {
                    Integer channel = data.getInt(0x0001);
                    HashMap<Integer, SubDataClient> subdata = Try.all.get(() -> Util.reflect(ExternalHost.class.getDeclaredField("subdata"), host));
                    if (!subdata.keySet().contains(channel) || (channel == 0 && subdata.get(0) == null)) {
                        ((ExternalHost) host).setSubData(client, channel);
                        Logger.get("SubData").info(client.getAddress().toString() + " has been defined as Host: " + host.getName() + ((channel > 0)?" [+"+channel+"]":""));
                        queue(host.getName(), () -> client.sendPacket(new PacketLinkExHost(0, null)));
                        setReady(client);
                    } else {
                        client.sendPacket(new PacketLinkExHost(3, "Host already linked"));
                    }
                } else {
                    client.sendPacket(new PacketLinkExHost(4, "That host does not support a network interface"));
                }
            } else {
                client.sendPacket(new PacketLinkExHost(2, "There is no host with name: " + data.getString(0x0000)));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketLinkExHost(1, null));
            e.printStackTrace();
        }
    }

    private void queue(String name, Runnable action) {
        final long now = Calendar.getInstance().getTime().getTime();
        Timer timer = new Timer("SubServers.Bungee::ExHost_Linker(" + name + ")");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                action.run();
                --req;
                timer.cancel();
            }
        }, (now - last < 500) ? (req * 500) : 0);

        ++req;
        last = now;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
