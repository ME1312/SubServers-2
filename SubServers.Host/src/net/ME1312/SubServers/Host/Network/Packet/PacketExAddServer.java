package net.ME1312.SubServers.Host.Network.Packet;

import com.dosse.upnp.UPnP;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Executable.Executable;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExAddServer implements PacketIn, PacketOut {
    private ExHost host;
    private int response;
    private String message;
    private String id;
    private Logger log = null;

    /**
     * New PacketExAddServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExAddServer(ExHost host) {
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
     * New PacketExAddServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param id Receiver ID
     */
    public PacketExAddServer(int response, String message, String id) {
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
            if (host.servers.keySet().contains(data.getSection("server").getRawString("name").toLowerCase())) {
                host.subdata.sendPacket(new PacketExAddServer(0, "Server Already Added", (data.contains("id"))?data.getRawString("id"):null));
            } else {
                SubServer server = new SubServer(host, data.getSection("server").getRawString("name"), data.getSection("server").getBoolean("enabled"), data.getSection("server").getInt("port"), data.getSection("server").getBoolean("log"),
                        data.getSection("server").getRawString("dir"), new Executable(data.getSection("server").getRawString("exec")), data.getSection("server").getRawString("stopcmd"));
                host.servers.put(data.getSection("server").getRawString("name").toLowerCase(), server);
                if (UPnP.isUPnPAvailable() && host.config.get().getSection("Settings").getSection("UPnP", new YAMLSection()).getBoolean("Forward-Servers", false)) UPnP.openPortTCP(server.getPort());
                log.info.println("Added SubServer: " + data.getSection("server").getRawString("name"));
                if (data.getSection("server").contains("running")) server.start(UUID.fromString(data.getSection("server").getRawString("running")));
                host.subdata.sendPacket(new PacketExAddServer(0, "Server Added Successfully", (data.contains("id"))?data.getRawString("id"):null));
            }
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExAddServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.contains("id"))?data.getRawString("id"):null));
            host.log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.13.1b");
    }
}