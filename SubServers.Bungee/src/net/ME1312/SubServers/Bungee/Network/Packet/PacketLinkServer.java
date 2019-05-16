package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.ServerContainer;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
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
        if (Util.isNull(response)) throw new NullPointerException();
        this.name = name;
        this.response = response;
        this.message = message;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, name);
        data.set(0x0001, response);
        if (message != null) data.set(0x0002, message);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        String name =  (data.contains(0x0000))?data.getRawString(0x0000):null;
        Integer port = (data.contains(0x0001))?data.getInt(0x0001):null;
        Integer channel = data.getInt(0x0002);

        try {
            Map<String, Server> servers = plugin.api.getServers();
            Server server;
            if (name != null && servers.keySet().contains(name.toLowerCase())) {
                link(client, servers.get(name.toLowerCase()), channel);
            } else if (port != null) {
                if ((server = search(new InetSocketAddress(client.getAddress().getAddress(), port))) != null) {
                    link(client, server, channel);
                } else {
                    throw new ServerLinkException("There is no server with address: " + client.getAddress().getAddress().getHostAddress() + ':' + port);
                }
            } else {
                throw new ServerLinkException("Not enough arguments");
            }
        } catch (ServerLinkException e) {
            if (name != null) {
                client.sendPacket(new PacketLinkServer(null, 3, "There is no server with name: " + name));
            } else {
                client.sendPacket(new PacketLinkServer(null, 2, e.getMessage()));
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketLinkServer(null, 1, null));
            e.printStackTrace();
        }
    }

    private void link(SubDataClient client, Server server, int channel) throws Throwable {
        HashMap<Integer, SubDataClient> subdata = Util.getDespiteException(() -> Util.reflect(ServerContainer.class.getDeclaredField("subdata"), server), null);
        if (!subdata.keySet().contains(channel) || (channel == 0 && subdata.get(0) == null)) {
            server.setSubData(client, channel);
            Logger.get("SubData").info(client.getAddress().toString() + " has been defined as " + ((server instanceof SubServer) ? "SubServer" : "Server") + ": " + server.getName() + ((channel > 0)?" (Sub-"+channel+")":""));
            if (server instanceof SubServer && !((SubServer) server).isRunning()) {
                Logger.get("SubServers").info("Sending shutdown signal to rogue SubServer: " + server.getName());
                client.sendPacket(new PacketOutExReset("Rogue SubServer Detected"));
            } else {
                client.sendPacket(new PacketLinkServer(server.getName(), 0, null));
            }
            setReady(client, true);
        } else {
            client.sendPacket(new PacketLinkServer(null, 4, "Server already linked"));
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
    public int version() {
        return 0x0001;
    }
}
