package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
        JSONObject templates = new JSONObject();
        for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) {
            try {
                JSONObject tinfo = new JSONObject();
                tinfo.put("enabled", template.isEnabled());
                tinfo.put("display", template.getDisplayName());
                tinfo.put("icon", template.getIcon());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                Util.zip(template.getDirectory(), bytes);
                tinfo.put("files", Base64.getEncoder().encodeToString(bytes.toByteArray()));
                tinfo.put("build", template.getBuildOptions().toJSON());
                tinfo.put("settings", template.getConfigOptions().toJSON());
                templates.put(template.getName(), tinfo);
            } catch (Exception e) {
                System.out.println("SubServers > Problem encoding template files: " + template.getName());
                e.printStackTrace();
            }
        }
        json.put("templates", templates);
        return json;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Client client, JSONObject data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getSection("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
