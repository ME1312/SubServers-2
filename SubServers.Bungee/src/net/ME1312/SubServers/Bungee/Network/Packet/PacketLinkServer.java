package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubStartedEvent;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.ServerImpl;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Host.SubServerImpl;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Link Server Packet
 */
public class PacketLinkServer implements InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    public static boolean strict = true;
    private SubProxy plugin;
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
    public PacketLinkServer(SubProxy plugin) {
        Util.nullpo(plugin);
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
        Util.nullpo(response);
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
        String name =  (data.contains(0x0000))?data.getString(0x0000):null;
        Integer channel = data.getInt(0x0002);
        InetSocketAddress address;

        try {
            if (!data.contains(0x0001)) {
                address = null;
            } else if (data.isNumber(0x0001)) {
                address = new InetSocketAddress(client.getAddress().getAddress(), data.getInt(0x0001));
            } else {
                String[] sa = data.getString(0x0001).split(":");
                address = new InetSocketAddress(sa[0], Integer.parseInt(sa[1]));
            }

            Server server;
            Map<String, Server> servers = plugin.api.getServers();
            if (name != null && servers.containsKey(name.toLowerCase())) {
                link(client, servers.get(name.toLowerCase()), channel);
            } else if (address != null) {
                if ((server = search(address)) != null || (server = create(name, address)) != null) {
                    link(client, server, channel);
                } else {
                    throw new ServerLinkException("There is no server with address: " + address.getAddress().getHostAddress() + ':' + address.getPort());
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

    private Server create(String name, InetSocketAddress address) throws Throwable {
        if (strict) {
            return null;
        } else {
            String id = (name == null)? Util.getNew(SubAPI.getInstance().getServers().keySet(), () -> UUID.randomUUID().toString()) : name;
            Server server = SubAPI.getInstance().addServer(id, address.getAddress(), address.getPort(), "Some Dynamic Server", name == null, false);
            if (server != null) Util.reflect(ServerImpl.class.getDeclaredField("persistent"), server, false);
            return server;
        }
    }

    private Server search(InetSocketAddress address) {
        Server server = null;
        for (Server s : plugin.api.getServers().values()) {
            if (s.getAddress().equals(address)) {
                if (server != null) throw new ServerLinkException("Multiple servers match address: " + address.getAddress().getHostAddress() + ':' + address.getPort());
                server = s;
            }
        }
        return server;
    }

    static long req = 1;
    static long last = Calendar.getInstance().getTime().getTime();
    private void link(SubDataClient client, Server server, int channel) throws Throwable {
        HashMap<Integer, SubDataClient> subdata = Try.all.get(() -> Util.reflect(ServerImpl.class.getDeclaredField("subdata"), server));
        if (!subdata.containsKey(channel) || (channel == 0 && subdata.get(0) == null)) {
            server.setSubData(client, channel);
            Logger.get("SubData").info(client.getAddress().toString() + " has been defined as " + ((server instanceof SubServer) ? "SubServer" : "Server") + ": " + server.getName() + ((channel > 0)?" [+"+channel+"]":""));
            Runnable register = () -> {
                if (server instanceof SubServer && !((SubServer) server).isRunning()) {
                    if (((SubServer) server).isAvailable()) {
                        Logger.get("SubServers").info("Sending shutdown signal to rogue SubServer: " + server.getName());
                        client.sendPacket(new PacketOutExReset("Rogue SubServer Detected"));
                    } else {
                        // Drop connection if host is unavailable for rogue checking (try again later)
                        Try.all.run(() -> Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.CLOSE_REQUESTED));
                    }
                } else {
                    if (server instanceof SubServer && !Try.all.get(() -> Util.reflect(SubServerImpl.class.getDeclaredField("started"), server), true)) {
                        Try.all.run(() -> Util.reflect(SubServerImpl.class.getDeclaredField("started"), server, true));
                        SubStartedEvent event = new SubStartedEvent((SubServer) server);
                        ProxyServer.getInstance().getPluginManager().callEvent(event);
                    }
                    client.sendPacket(new PacketLinkServer(server.getName(), 0, null));
                }
                --req;
            };

            final long now = Calendar.getInstance().getTime().getTime();
            Timer timer = new Timer("SubServers.Bungee::Server_Linker(" + server.getName() + ")");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    register.run();
                    timer.cancel();
                }
            }, ((server instanceof SubServer && !((SubServer) server).isRunning()) ? TimeUnit.SECONDS.toMillis(5) : 0) + ((now - last < 500) ? (req * 500) : 0));

            ++req;
            last = now;
            setReady(client);
        } else {
            client.sendPacket(new PacketLinkServer(null, 4, "Server already linked"));
        }
    }
}
