package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;

/**
 * Link External Host Packet
 */
public class PacketLinkExHost implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;

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
     * @param message Message
     */
    public PacketLinkExHost(int response, String message) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("r", response);
        data.set("m", message);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            Map<String, Host> hosts = plugin.api.getHosts();
            if (hosts.keySet().contains(data.getRawString("name").toLowerCase())) {
                Host host = hosts.get(data.getRawString("name").toLowerCase());
                if (host instanceof ClientHandler) {
                    if (((ClientHandler) host).getSubData() == null) {
                        client.setHandler((ClientHandler) host);
                        System.out.println("SubData > " + client.getAddress().toString() + " has been defined as Host: " + host.getName());
                        client.sendPacket(new PacketLinkExHost(0, "Definition Successful"));
                    } else {
                        client.sendPacket(new PacketLinkExHost(3, "Host already linked"));
                    }
                } else {
                    client.sendPacket(new PacketLinkExHost(4, "That host does not support a network interface"));
                }
            } else {
                client.sendPacket(new PacketLinkExHost(2, "There is no host with name: " + data.getRawString("name")));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketLinkExHost(1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
