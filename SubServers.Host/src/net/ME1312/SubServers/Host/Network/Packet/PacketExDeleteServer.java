package net.ME1312.SubServers.Host.Network.Packet;

import com.dosse.upnp.UPnP;
import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;

/**
 * Create Server Packet
 */
public class PacketExDeleteServer implements PacketIn, PacketOut {
    private ExHost host;
    private int response;
    private String message;
    private String id;
    private Logger log = null;

    /**
     * New PacketExDeleteServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExDeleteServer(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    /**
     * New PacketExDeleteServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketExDeleteServer(int response, String message, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
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
    public void execute(YAMLSection data) {
        try {
            if (!host.servers.keySet().contains(data.getRawString("server").toLowerCase())) {
                host.subdata.sendPacket(new PacketExDeleteServer(0, "Server Didn't Exist", (data.contains("id"))?data.getRawString("id"):null));
            } else if (host.servers.get(data.getRawString("server").toLowerCase()).isRunning()) {
                host.subdata.sendPacket(new PacketExDeleteServer(2, "That server is still running.", (data.contains("id"))?data.getRawString("id"):null));
            } else {
                SubServer server = host.servers.get(data.getRawString("server").toLowerCase());
                host.servers.remove(data.getRawString("server").toLowerCase());
                new Thread(() -> {
                    UniversalFile to = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Recently Deleted:" + server.getName().toLowerCase());
                    try {
                        File from = new File(host.host.getRawString("Directory"), server.getDirectory());
                        if (from.exists()) {
                            log.info.println("Removing Files...");
                            if (to.exists()) {
                                if (to.isDirectory()) Util.deleteDirectory(to);
                                else to.delete();
                            }
                            to.mkdirs();
                            Util.copyDirectory(from, to);
                            Util.deleteDirectory(from);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    log.info.println("Saving...");
                    try {
                        if (!to.exists()) to.mkdirs();
                        FileWriter writer = new FileWriter(new File(to, "info.json"));
                        data.getSection("info").toJSON().write(writer);
                        writer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getPort())) UPnP.closePortTCP(server.getPort());
                    log.info.println("Deleted SubServer: " + data.getRawString("server"));
                    host.subdata.sendPacket(new PacketExDeleteServer(0, "Server Deleted Successfully", (data.contains("id"))?data.getRawString("id"):null));
                }).start();
            }
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExDeleteServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.contains("id"))?data.getRawString("id"):null));
            host.log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}