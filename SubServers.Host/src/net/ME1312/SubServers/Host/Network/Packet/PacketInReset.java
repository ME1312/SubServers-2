package net.ME1312.SubServers.Host.Network.Packet;


import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.ExHost;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Reset Packet
 */
public class PacketInReset implements PacketIn {
    private ExHost host;

    public PacketInReset(ExHost host) {
        this.host = host;
    }

    @Override
    public void execute(JSONObject data) {
        if (data != null && data.keySet().contains("m")) {
            List<String> subservers = new ArrayList<String>();
            subservers.addAll(host.servers.keySet());

            for (String server : subservers) {
                host.servers.get(server).stop();
                try {
                    host.servers.get(server).waitFor();
                } catch (Exception e) {
                    host.log.error.println(e);
                }
            }
            subservers.clear();
            host.servers.clear();

            if (host.creator.isBusy()) {
                host.creator.terminate();
                try {
                    host.creator.waitFor();
                } catch (Exception e) {
                    host.log.error.println(e);
                }
            }
            host.templates.clear();

            if (new UniversalFile(host.dir, "Templates").exists()) for (File file : new UniversalFile(host.dir, "Templates").listFiles()) {
                try {
                    if (file.isDirectory()) {
                        YAMLSection config = (new UniversalFile(file, "template.yml").exists())?new YAMLConfig(new UniversalFile(file, "template.yml")).get().getSection("Template", new YAMLSection()):new YAMLSection();
                        SubCreator.ServerTemplate template = new SubCreator.ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getSection("Build", new YAMLSection()), config.getSection("Settings", new YAMLSection()));
                        host.templates.put(file.getName().toLowerCase(), template);
                        if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                    }
                } catch (Exception e) {
                    System.out.println("SubCreator > Couldn't load template: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
