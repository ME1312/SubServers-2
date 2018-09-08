package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import org.msgpack.value.Value;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketOut {
    private static boolean first = false;
    private ExHost host;
    private Logger log = null;

    /**
     * New PacketExConfigureHost
     */
    public PacketExConfigureHost(ExHost host) {
        this.host = host;
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    @Override
    public YAMLSection generate() {
        host.log.info.println("Downloading Host Settings...");
        first = true;
        return null;
    }

    @Override
    public void execute(YAMLSection data) {
        host.host = data.getSection("host").clone();
        for (SubCreator.ServerTemplate template : host.templates.values()) {
            Util.deleteDirectory(template.getDirectory());
        }
        host.templates.clear();
        UniversalFile templates = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Templates");
        Util.deleteDirectory(templates);
        templates.mkdirs();
        for (String name : data.getSection("templates").getKeys()) {
            try {
                UniversalFile dir = new UniversalFile(templates, name);
                dir.mkdirs();
                Util.unzip(new ByteArrayInputStream(((Value) data.getSection("templates").getSection(name).getObject("files")).asBinaryValue().asByteArray()), dir);
                SubCreator.ServerTemplate template = new SubCreator.ServerTemplate(name, data.getSection("templates").getSection(name).getBoolean("enabled"), data.getSection("templates").getSection(name).getRawString("icon"), dir,
                        data.getSection("templates").getSection(name).getSection("build").clone(), data.getSection("templates").getSection(name).getSection("settings").clone());
                host.templates.put(name.toLowerCase(), template);
                if (!data.getSection("templates").getSection(name).getRawString("display").equals(name)) template.setDisplayName(data.getSection("templates").getSection(name).getRawString("display"));
            } catch (Exception e) {
                host.log.error.println("Couldn't load template: " + name);
                host.log.error.println(e);
            }
        }
        log.info.println(((first)?"":"New ") + "Host Settings Downloaded");
        first = false;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
