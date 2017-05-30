package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private ExternalHost host;

    /**
     * New PacketExConfigureHost (In)
     */
    public PacketExConfigureHost(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketExConfigureHost (Out)
     */
    public PacketExConfigureHost(SubPlugin plugin, ExternalHost host) {
        this.plugin = plugin;
        this.host = host;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("host", plugin.config.get().getSection("Hosts").getSection(host.getName()).toJSON());
        return json;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Client client, JSONObject data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getSection("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            try {
                Field field = ExternalSubCreator.class.getDeclaredField("templates");
                field.setAccessible(true);
                HashMap<String, SubCreator.ServerTemplate> templates = new HashMap<String, SubCreator.ServerTemplate>();
                for (String name : data.getJSONObject("templates").keySet()) {
                    try {
                        SubCreator.ServerTemplate template = new SubCreator.ServerTemplate(name, data.getJSONObject("templates").getJSONObject(name).getBoolean("enabled"), data.getJSONObject("templates").getJSONObject(name).getString("icon"),
                                new File(data.getJSONObject("templates").getJSONObject(name).getString("dir")), new YAMLSection(data.getJSONObject("templates").getJSONObject(name).getJSONObject("build")), new YAMLSection(data.getJSONObject("templates").getJSONObject(name).getJSONObject("options")));
                        templates.put(name.toLowerCase(), template);
                        if (data.getJSONObject("templates").getJSONObject(name).keySet().contains("display")) template.setDisplayName(data.getJSONObject("templates").getJSONObject(name).getString("display"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                field.set(((ExternalHost) client.getHandler()).getCreator(), templates);
                field.setAccessible(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
