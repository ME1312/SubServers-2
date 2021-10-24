package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.ChatColor;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

/**
 * Add Server Packet
 */
public class PacketAddServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private int response;
    private int status;
    private UUID tracker;

    /**
     * New PacketAddServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketAddServer(SubProxy plugin) {
        Util.nullpo(plugin);
        this.plugin = plugin;
    }

    /**
     * New PacketEditServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketAddServer(int response, UUID tracker) {
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        if (tracker != null) json.set(0x0000, tracker);
        json.set(0x0001, response);
        json.set(0x0002, status);
        return json;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        UUID tracker =     (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =                    data.getRawString(0x0001);
            boolean subserver =                data.getBoolean(0x0002);
            ObjectMap<String> opt = new ObjectMap<>((Map<String, ?>)data.getObject(0x0003));
            UUID player =                      (data.contains(0x0004)?data.getUUID(0x0004):null);

            if (plugin.api.getServers().keySet().contains(name.toLowerCase())) {
                client.sendPacket(new PacketAddServer(3, tracker));
            } else {
                if (!subserver) {
                    if (plugin.api.addServer(player, name, InetAddress.getByName(opt.getRawString("address").split(":")[0]), Integer.parseInt(opt.getRawString("address").split(":")[1]),
                            ChatColor.translateAlternateColorCodes('&', opt.getString("motd")), opt.getBoolean("hidden"), opt.getBoolean("restricted")) != null) {
                        client.sendPacket(new PacketAddServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketAddServer(1, tracker));
                    }
                } else if (!plugin.api.getHosts().keySet().contains(opt.getRawString("host").toLowerCase())) {
                    client.sendPacket(new PacketAddServer(4, tracker));
                } else {
                    if (plugin.api.getHost(opt.getRawString("host")).addSubServer(player, name, opt.getBoolean("enabled"), opt.getInt("port"), ChatColor.translateAlternateColorCodes('&', opt.getString("motd")),
                            opt.getBoolean("log"), opt.getRawString("dir"), opt.getRawString("exec"), opt.getRawString("stop-cmd"), opt.getBoolean("hidden"), opt.getBoolean("restricted")) != null) {
                        client.sendPacket(new PacketAddServer(0, tracker));
                    } else {
                        client.sendPacket(new PacketAddServer(1, tracker));
                    }
                }
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketAddServer(2, tracker));
            e.printStackTrace();
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
