package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Proxy;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.Map;
import java.util.UUID;

/**
 * Link Proxy Packet
 */
public class PacketLinkProxy implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String name;

    /**
     * New PacketLinkProxy (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkProxy(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkProxy (Out)
     *
     * @param name The name that was generated
     * @param response Response ID
     * @param message Message
     */
    public PacketLinkProxy(String name, int response, String message) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.name = name;
        this.response = response;
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("n", name);
        json.set("r", response);
        json.set("m", message);
        return json;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            Map<String, Proxy> proxies = plugin.api.getProxies();
            Proxy proxy = new Proxy((data.contains("name") && !proxies.keySet().contains(data.getRawString("name").toLowerCase()))?data.getRawString("name"):Util.getNew(proxies.keySet(), () -> UUID.randomUUID().toString()));
            plugin.proxies.put(proxy.getName().toLowerCase(), proxy);
            client.setHandler(proxy);
            System.out.println("SubData > " + client.getAddress().toString() + " has been defined as Proxy: " + proxy.getName());
            client.sendPacket(new PacketLinkProxy(proxy.getName(), 0, "Definition Successful"));
        } catch (Exception e) {
            client.sendPacket(new PacketLinkProxy(null, 1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
