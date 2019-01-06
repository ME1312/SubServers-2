package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketCreateServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String id;

    /**
     * New PacketCreateServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketCreateServer(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketCreateServer(int response, String message, String id) {
        this.response = response;
        this.message = message;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);
        data.set("r", response);
        data.set("m", message);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            if (data.getSection("creator").getString("name").contains(" ")) {
                client.sendPacket(new PacketCreateServer(3, "Server names cannot have spaces", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (plugin.api.getSubServers().keySet().contains(data.getSection("creator").getString("name").toLowerCase()) || SubCreator.isReserved(data.getSection("creator").getString("name"))) {
                client.sendPacket(new PacketCreateServer(4, "There is already a subserver with that name", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (!plugin.hosts.keySet().contains(data.getSection("creator").getString("host").toLowerCase())) {
                client.sendPacket(new PacketCreateServer(5, "There is no Host with that name", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (!plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).isAvailable()) {
                client.sendPacket(new PacketStartServer(6, "That SubServer's Host is not available", (data.contains("id"))?data.getRawString("id"):null));
            } else if (!plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).isEnabled()) {
                client.sendPacket(new PacketStartServer(7, "That SubServer's Host is not enabled", (data.contains("id"))?data.getRawString("id"):null));
            } else if (!plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).getCreator().getTemplates().keySet().contains(data.getSection("creator").getString("template").toLowerCase())) {
                client.sendPacket(new PacketCreateServer(8, "There is no template with that name", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (!plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).getCreator().getTemplate(data.getSection("creator").getString("template")).isEnabled()) {
                client.sendPacket(new PacketCreateServer(8, "That Template is not enabled", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (new Version("1.8").compareTo(new Version(data.getSection("creator").getString("version"))) > 0) {
                client.sendPacket(new PacketCreateServer(10, "SubCreator cannot create servers before Minecraft 1.8", (data.contains("id")) ? data.getRawString("id") : null));
            } else if (data.getSection("creator").contains("port") && (data.getSection("creator").getInt("port") <= 0 || data.getSection("creator").getInt("port") > 65535)) {
                client.sendPacket(new PacketCreateServer(11, "Invalid Port Number", (data.contains("id")) ? data.getRawString("id") : null));
            } else {
                if (plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).getCreator().create((data.contains("player"))?UUID.fromString(data.getRawString("player")):null, data.getSection("creator").getString("name"), plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).getCreator().getTemplate(data.getSection("creator").getString("template")), new Version(data.getSection("creator").getString("version")), (data.getSection("creator").contains("port"))?data.getSection("creator").getInt("port"):null)) {
                    if (data.contains("wait") && data.getBoolean("wait")) {
                        new Thread(() -> {
                            try {
                                plugin.hosts.get(data.getSection("creator").getString("host").toLowerCase()).getCreator().waitFor();
                                client.sendPacket(new PacketCreateServer(0, "Created SubServer", (data.contains("id")) ? data.getRawString("id") : null));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }, "SubServers.Bungee::SubData_SubCreator_Handler(" + client.getAddress().toString() + ')').start();
                    } else {
                        client.sendPacket(new PacketCreateServer(0, "Creating SubServer", (data.contains("id")) ? data.getRawString("id") : null));
                    }
                } else {
                    client.sendPacket(new PacketCreateServer(1, "Couldn't create SubServer", (data.contains("id")) ? data.getRawString("id") : null));
                }

            }
        } catch (Throwable e) {
            client.sendPacket(new PacketCreateServer(2, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.contains("id")) ? data.getRawString("id") : null));
            e.printStackTrace();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.13b");
    }
}
