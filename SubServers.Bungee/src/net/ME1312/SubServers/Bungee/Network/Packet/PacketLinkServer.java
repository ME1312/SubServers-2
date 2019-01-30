package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private int response;
    private String message;
    private String name;

    private static class ServerLinkException extends IllegalStateException {
        public ServerLinkException(String message) {
            super(message);
        }
    }

    /**
     * New PacketLinkServer (In)
     *
     * @param plugin SubPlugin
     */
    public PacketLinkServer(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketLinkServer (Out)
     *
     * @param name The name that was determined
     * @param response Response ID
     * @param message Message
     */
    public PacketLinkServer(String name, int response, String message) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.name = name;
        this.response = response;
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("n", name);
        data.set("r", response);
        data.set("m", message);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            Map<String, Server> servers = plugin.api.getServers();
            Server server;
            if (data.contains("name") && servers.keySet().contains(data.getRawString("name").toLowerCase())) {
                link(client, servers.get(data.getRawString("name").toLowerCase()));
            } else if (data.contains("port")) {
                if ((server = search(new InetSocketAddress(client.getAddress().getAddress(), data.getInt("port")))) != null) {
                    link(client, server);
                } else {
                    throw new ServerLinkException("There is no server with address: " + client.getAddress().getAddress().getHostAddress() + ':' + data.getInt("port"));
                }
            } else {
                throw new ServerLinkException("Not enough arguments");
            }
        } catch (ServerLinkException e) {
            if (data.contains("name")) {
                client.sendPacket(new PacketLinkServer(null, 2, "There is no server with name: " + data.getRawString("name")));
            } else {
                client.sendPacket(new PacketLinkServer(null, 2, e.getMessage()));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketLinkServer(null, 1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void link(Client client, Server server) {
        if (server.getSubData() == null) {
            client.setHandler(server);
            System.out.println("SubData > " + client.getAddress().toString() + " has been defined as " + ((server instanceof SubServer) ? "SubServer" : "Server") + ": " + server.getName());
            if (server instanceof SubServer && !((SubServer) server).isRunning()) {
                System.out.println("SubServers > Sending shutdown signal to rogue SubServer: " + server.getName());
                client.sendPacket(new PacketOutReset("Rogue SubServer Detected"));
            } else {
                client.sendPacket(new PacketLinkServer(server.getName(), 0, "Definition Successful"));
            }
        } else {
            client.sendPacket(new PacketLinkServer(null, 3, "Server already linked"));
        }
    }

    private Server search(InetSocketAddress address) throws ServerLinkException {
        Server server = null;
        for (Server s : plugin.api.getServers().values()) {
            if (s.getAddress().equals(address)) {
                if (server != null) throw new ServerLinkException("Multiple servers match address: " + address.getAddress().getHostAddress() + ':' + address.getPort());
                server = s;
            }
        }
        return server;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
