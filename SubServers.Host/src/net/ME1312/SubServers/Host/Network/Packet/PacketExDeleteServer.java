package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.SubAPI;

import com.dosse.upnp.UPnP;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Create Server Packet
 */
public class PacketExDeleteServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int response;
    private UUID tracker;

    /**
     * New PacketExDeleteServer (In)
     *
     * @param host ExHost
     */
    public PacketExDeleteServer(ExHost host) {
        Util.nullpo(host);
        this.host = host;
    }

    /**
     * New PacketExDeleteServer (Out)
     *
     * @param response Response ID
     * @param tracker Receiver ID
     */
    public PacketExDeleteServer(int response, UUID tracker) {
        Util.nullpo(response);
        this.response = response;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        Logger log = Try.all.get(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.getConnection()), null);
        UUID tracker =                     (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            String name =               data.getString(0x0001);
            YAMLSection info = new YAMLSection((Map<String, ?>) data.getObject(0x0002));
            boolean recycle =             data.getBoolean(0x0003, false);

            if (!host.servers.keySet().contains(name.toLowerCase())) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExDeleteServer(1, tracker));
            } else if (host.servers.get(name.toLowerCase()).isRunning()) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExDeleteServer(3, tracker));
            } else {
                SubServerImpl server = host.servers.get(name.toLowerCase());
                host.servers.remove(name.toLowerCase());
                new Thread(() -> {
                    File to = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Recently Deleted/" + server.getName().toLowerCase());
                    try {
                        File from = new File(host.host.getString("Directory"), server.getPath());
                        if (from.exists()) {
                            log.info("Removing Files...");
                            if (recycle) {
                                if (to.exists()) {
                                    if (to.isDirectory()) Directories.delete(to);
                                    else to.delete();
                                }
                                to.mkdirs();
                                Directories.copy(from, to);
                            }
                            Directories.delete(from);
                        }
                    } catch (Exception e) {
                        SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
                    }

                    log.info("Saving...");
                    if (recycle) try {
                        if (!to.exists()) to.mkdirs();
                        FileWriter writer = new FileWriter(new File(to, "info.json"));
                        info.toJSON().write(writer);
                        writer.close();
                    } catch (Exception e) {
                        SubAPI.getInstance().getAppInfo().getLogger().error.println(e);
                    }
                    if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getPort())) UPnP.closePortTCP(server.getPort());
                    log.info("Deleted SubServer: " + name);
                    ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExDeleteServer(0, tracker));
                }, SubAPI.getInstance().getAppInfo().getName() + "::Server_Deletion(" + server.getName() + ')').start();
            }
        } catch (Throwable e) {
            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExDeleteServer(2, tracker));
            host.log.error.println(e);
        }
    }
}