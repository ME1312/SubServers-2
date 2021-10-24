package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl.ServerTemplate;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * Create Server Packet
 */
public class PacketExCreateServer implements PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private ExHost host;
    private int response;
    private String message;
    private Map<String, ?> info;
    private UUID tracker;

    /**
     * New PacketExCreateServer (In)
     *
     * @param host ExHost
     */
    public PacketExCreateServer(ExHost host) {
        Util.nullpo(host);
        this.host = host;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param tracker Receiver ID
     */
    public PacketExCreateServer(int response, String message, UUID tracker) {
        Util.nullpo(response);
        this.response = response;
        this.message = message;
        this.tracker = tracker;
    }

    /**
     * New PacketCreateServer (Out)
     *
     * @param response Response ID
     * @param message Message
     * @param info Creator Info
     * @param tracker Receiver ID
     */
    public PacketExCreateServer(int response, String message, Map<String, ?> info, UUID tracker) {
        Util.nullpo(response);
        this.response = response;
        this.message = message;
        this.info = info;
        this.tracker = tracker;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (tracker != null) data.set(0x0000, tracker);
        data.set(0x0001, response);
        if (info != null) data.set(0x0002, info);
        if (message != null) data.set(0x0003, message);
        return data;
    }

    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        UUID tracker =             (data.contains(0x0000)?data.getUUID(0x0000):null);
        try {
            if (data.contains(0x0001)) {
                if (data.contains(0x0001)) {
                    host.creator.terminate(data.getString(0x0001).toLowerCase());
                } else {
                    host.creator.terminate();
                }
                client.sendPacket(new PacketExCreateServer(1, null, tracker));
            } else {
                String name =    data.getString(0x0002);
                ObjectMapValue<Integer> template = data.get(0x0003);
                Version version =   (data.contains(0x0004)?data.getVersion(0x0004):null);
                Integer port =         data.getInt(0x0005);
                UUID log =            data.getUUID(0x0006);
                Boolean mode =      (data.contains(0x0007)?data.getBoolean(0x0007):null);
                UUID player =       (data.contains(0x0008)?data.getUUID(0x0008):null);

                SubCreatorImpl.ServerTemplate templateV;

                if (template.isString()) {
                    templateV = host.templates.get(template.asString().toLowerCase());
                    if (templateV == null) templateV = host.templatesR.get(template.asString().toLowerCase());
                } else {
                    ObjectMap<String> templateM = template.asMap().key();
                    templateV = new ServerTemplate(
                            templateM.getString("name"),
                            templateM.getBoolean("enabled"),
                            templateM.getString("icon"),
                            new File(templateM.getString("dir").replace('/', File.separatorChar)),
                            templateM.getMap("build"),
                            templateM.getMap("def"));
                    templateV.setDisplayName(templateM.getString("display"));
                }

                host.creator.create(player, name, templateV, version, port, mode, log, tracker);
            }
        } catch (Throwable e) {
            host.log.error.println(e);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}