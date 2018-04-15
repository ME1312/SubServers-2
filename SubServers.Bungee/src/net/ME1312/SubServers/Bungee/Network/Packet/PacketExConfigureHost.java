package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.io.*;
import java.util.Base64;

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
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("host", plugin.config.get().getSection("Hosts").getSection(host.getName()).clone());
        YAMLSection templates = new YAMLSection();
        for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) {
            try {
                YAMLSection tinfo = new YAMLSection();
                tinfo.set("enabled", template.isEnabled());
                tinfo.set("display", template.getDisplayName());
                tinfo.set("icon", template.getIcon());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                Util.zip(template.getDirectory(), bytes);
                tinfo.set("files", Base64.getEncoder().encodeToString(bytes.toByteArray()));
                tinfo.set("build", template.getBuildOptions().clone());
                tinfo.set("settings", template.getConfigOptions().clone());
                templates.set(template.getName(), tinfo);
            } catch (Exception e) {
                System.out.println("SubServers > Problem encoding template files: " + template.getName());
                e.printStackTrace();
            }
        }
        data.set("templates", templates);
        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Client client, YAMLSection data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getSection("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
