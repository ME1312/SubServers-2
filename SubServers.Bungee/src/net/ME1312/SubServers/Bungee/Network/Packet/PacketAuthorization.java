package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.ME1312.SubServers.Bungee.SubPlugin;

/**
 * Authorization Packet
 */
public final class PacketAuthorization implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;

    /**
     * New PacketAuthorization (In)
     *
     * @param plugin SubPlugin
     */
    public PacketAuthorization(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketAuthorization (Out)
     *
     * @param response Response ID
     * @param message Message
     */
    public PacketAuthorization(int response, String message) {
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
            if (data.getRawString("password").equals(Util.reflect(SubDataServer.class.getDeclaredField("password"), plugin.subdata))) {
                client.authorize();
                client.sendPacket(new PacketAuthorization(0, "Successfully Logged in"));
            } else {
                client.sendPacket(new PacketAuthorization(2, "Invalid Password"));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketAuthorization(1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
