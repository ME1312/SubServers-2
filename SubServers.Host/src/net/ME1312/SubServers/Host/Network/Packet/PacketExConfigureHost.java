package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Base64;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketOut {
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
    public JSONObject generate() {
        return null;
    }

    @Override
    public void execute(JSONObject data) {
        host.host = new YAMLSection(data.getJSONObject("host"));
        UniversalFile templates = new UniversalFile(host.runtime, "net:ME1312:SubServers:Host:Library:Files:Templates");
        templates.mkdirs();
        for (String name : data.getJSONObject("templates").keySet()) {
            try {
                UniversalFile dir = new UniversalFile(templates, name);
                Util.unzip(new ByteArrayInputStream(Base64.getDecoder().decode(data.getJSONObject("templates").getJSONObject(name).getString("files"))), dir);
                SubCreator.ServerTemplate template = new SubCreator.ServerTemplate(name, data.getJSONObject("templates").getJSONObject(name).getBoolean("enabled"), data.getJSONObject("templates").getJSONObject(name).getString("icon"), dir,
                        new YAMLSection(data.getJSONObject("templates").getJSONObject(name).getJSONObject("build")), new YAMLSection(data.getJSONObject("templates").getJSONObject(name).getJSONObject("settings")));
                host.templates.put(name.toLowerCase(), template);
                if (!data.getJSONObject("templates").getJSONObject(name).getString("display").equals(name)) template.setDisplayName(data.getJSONObject("templates").getJSONObject(name).getString("display"));
            } catch (Exception e) {
                host.log.error.println("Couldn't load template: " + name);
                host.log.error.println(e);
            }
        }
        log.info.println("Host Settings Downloaded");
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
