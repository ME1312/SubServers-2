package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.msgpack.value.ValueFactory;

import java.io.*;

/**
 * External Host Configuration Packet
 */
public class PacketExConfigureHost implements PacketIn, PacketObjectOut<Integer> {
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
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, plugin.config.get().getMap("Hosts").getMap(host.getName()).clone());
        ObjectMap<String> templates = new ObjectMap<String>();
        for (SubCreator.ServerTemplate template : host.getCreator().getTemplates().values()) {
            ObjectMap<String> tinfo = new ObjectMap<String>();
            tinfo.set("enabled", template.isEnabled());
            tinfo.set("display", template.getDisplayName());
            tinfo.set("icon", template.getIcon());
            tinfo.set("build", template.getBuildOptions().clone());
            tinfo.set("settings", template.getConfigOptions().clone());
            templates.set(template.getName(), tinfo);
        }
        data.set(0x0001, templates);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost && plugin.config.get().getMap("Hosts").getKeys().contains(((ExternalHost) client.getHandler()).getName())) {
            client.sendPacket(new PacketExConfigureHost(plugin, (ExternalHost) client.getHandler()));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
