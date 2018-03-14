package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

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
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("r", response);
        json.put("m", message);
        return json;
    }

    @Override
    public void execute(JSONObject data) {
        try {
            if (!host.servers.keySet().contains(data.getString("server").toLowerCase())) {
                host.subdata.sendPacket(new PacketExDeleteServer(0, "Server Didn't Exist", (data.keySet().contains("id"))?data.getString("id"):null));
            } else if (host.servers.get(data.getString("server").toLowerCase()).isRunning()) {
                host.subdata.sendPacket(new PacketExDeleteServer(2, "That server is still running.", (data.keySet().contains("id"))?data.getString("id"):null));
            } else {
                SubServer server = host.servers.get(data.getString("server").toLowerCase());
                host.servers.remove(data.getString("server").toLowerCase());
                new Thread(() -> {
                    UniversalFile to = new UniversalFile(host.dir, "Recently Deleted:" + server.getName().toLowerCase());
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
                        data.getJSONObject("info").write(writer);
                        writer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.info.println("Deleted SubServer: " + data.getString("server"));
                    host.subdata.sendPacket(new PacketExDeleteServer(0, "Server Deleted Successfully", (data.keySet().contains("id"))?data.getString("id"):null));
                }).start();
            }
        } catch (Throwable e) {
            host.subdata.sendPacket(new PacketExDeleteServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), (data.keySet().contains("id"))?data.getString("id"):null));
            host.log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}