package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;
import net.ME1312.SubServers.Host.ExHost;

import java.util.Map;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketObjectIn<Integer>, PacketOut {
    private static boolean first = false;
    private ExHost host;

    /**
     * New PacketExConfigureHost
     */
    public PacketExConfigureHost(ExHost host) {
        this.host = host;
    }

    @Override
    public void sending(SubDataClient client) {
        host.log.info.println("Downloading Host Settings...");
        first = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        host.host = new ObjectMap<>((Map<String, ?>) data.getObject(0x0000));
        for (SubCreatorImpl.ServerTemplate template : host.templates.values()) {
            Util.deleteDirectory(template.getDirectory());
        }
        host.templates.clear();
        UniversalFile templatedir = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Templates");
        ObjectMap<String> templates = new ObjectMap<>((Map<String, ?>) data.getObject(0x0001));
        Util.deleteDirectory(templatedir);
        templatedir.mkdirs();
        for (String name : templates.getKeys()) {
            try {
                UniversalFile dir = new UniversalFile(templatedir, name);
                SubCreatorImpl.ServerTemplate template = new SubCreatorImpl.ServerTemplate(name, templates.getMap(name).getBoolean("enabled"), templates.getMap(name).getRawString("icon"), dir,
                        templates.getMap(name).getMap("build").clone(), templates.getMap(name).getMap("settings").clone());
                host.templates.put(name.toLowerCase(), template);
                if (!templates.getMap(name).getRawString("display").equals(name)) template.setDisplayName(templates.getMap(name).getRawString("display"));
            } catch (Exception e) {
                host.log.error.println("Couldn't load template: " + name);
                host.log.error.println(e);
            }
        }
        host.log.info.println(((first)?"":"New ") + "Host Settings Downloaded");
        first = false;
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
