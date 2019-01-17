package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.ExHost;

import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExCreateServer implements PacketIn, PacketOut {
    private ExHost host;
    private int response;
    private String message;
    private YAMLSection info;
    private String id;

    /**
     * New PacketExCreateServer (In)
     *
     * @param host SubPlugin
     */
    public PacketExCreateServer(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param info Creator Info
     * @param id Receiver ID
     */
    public PacketExCreateServer(int response, String message, YAMLSection info, String id) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
        this.info = info;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);
        data.set("r", response);
        data.set("m", message);
        data.set("c", info);
        return data;
    }

    @Override
    public void execute(YAMLSection data) {
        try {
            host.creator.create(data.getSection("creator").getRawString("name"), host.templates.get(data.getSection("creator").getRawString("template").toLowerCase()), new Version(data.getSection("creator").getRawString("version")),
                    data.getSection("creator").getInt("port"), data.getSection("creator").getUUID("log"), (data.contains("id"))?data.getRawString("id"):null);
        } catch (Throwable e) {
            if (data.contains("thread")) {
                host.creator.terminate(data.getRawString("thread").toLowerCase());
            } else {
                host.creator.terminate();
            }
            host.subdata.sendPacket(new PacketExCreateServer(1, e.getClass().getCanonicalName() + ": " + e.getMessage(), null, (data.contains("id"))?data.getRawString("id"):null));
            host.log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}