package net.ME1312.SubServers.Host.Network.Packet;

import com.dosse.upnp.UPnP;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.ExHost;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Create Server Packet
 */
public class PacketExAddServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int response;
    private UUID running;
    private UUID tracker;

    /**
     * New PacketExAddServer (In)
     *
     * @param host ExHost
     */
    public PacketExAddServer(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketExAddServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExAddServer(int response, UUID tracker) {
        this(response, null, tracker);
    }

    /**
     * New PacketExAddServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExAddServer(int response, UUID running, UUID tracker) {
        if (Util.isNull(response)) throw new NullPointerException();
        this.response = response;
        this.tracker = tracker;
        this.running = running;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        if (running != null) data.set(0x0002, running);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger logger = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        UUID tracker =          (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =    data.getRawString(0x0001);
            boolean enabled =  data.getBoolean(0x0002);
            int port =             data.getInt(0x0003);
            boolean log =      data.getBoolean(0x0004);
            String dir =     data.getRawString(0x0005);
            String exec =    data.getRawString(0x0006);
            String stopcmd = data.getRawString(0x0007);
            UUID running =       data.contains(0x0008)?data.getUUID(0x0008):null;

            if (host.servers.keySet().contains(name.toLowerCase())) {
                SubServerImpl server = host.servers.get(name.toLowerCase());
                if (server.getPort() == port && server.getExecutable().equals(exec) && server.getDirectory().equals(dir)) {
                    if (server.isEnabled() != enabled || server.getLogger().isLogging() != log || !server.getStopCommand().equals(stopcmd)) {
                        server.setEnabled(enabled);
                        server.setLogging(log);
                        server.setStopCommand(stopcmd);
                        logger.info("Re-Added SubServer: " + server.getName());
                    }
                    client.sendPacket(new PacketExAddServer(0, (server.isRunning())?server.getLogger().getAddress():null, tracker));
                } else {
                    server.stop();
                    server.waitFor();
                    if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getPort()))
                        UPnP.closePortTCP(server.getPort());

                    init(client.getConnection(), server = new SubServerImpl(host, name, enabled, port, log, dir, exec, stopcmd), running, tracker, logger);
                }
            } else {
                init(client.getConnection(), new SubServerImpl(host, name, enabled, port, log, dir, exec, stopcmd), running, tracker, logger);
            }
        } catch (Throwable e) {
            client.sendPacket(new PacketExAddServer(2, tracker));
            host.log.error.println(e);
        }
    }

    private void init(SubDataClient client, SubServerImpl server, UUID running, UUID tracker, Logger logger) {
        host.servers.put(server.getName().toLowerCase(), server);
        if (UPnP.isUPnPAvailable() && host.config.get().getMap("Settings").getMap("UPnP", new ObjectMap<String>()).getBoolean("Forward-Servers", false)) UPnP.openPortTCP(server.getPort());
        logger.info("Added SubServer: " + server.getName());
        if (running != null) server.start(running);
        client.sendPacket(new PacketExAddServer(0, tracker));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}