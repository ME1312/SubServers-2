package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * External Host Template Upload Packet
 */
public class PacketExUploadTemplates implements PacketObjectIn<Integer>, PacketOut {
    private SubProxy plugin;

    /**
     * New PacketExUploadTemplates
     */
    public PacketExUploadTemplates(SubProxy plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost) {
            HashMap<String, SubCreator.ServerTemplate> map = Util.getDespiteException(() -> Util.reflect(ExternalSubCreator.class.getDeclaredField("templates"), ((ExternalHost) client.getHandler()).getCreator()), new HashMap<>());
            UniversalFile templatedir = new UniversalFile(plugin.dir, "SubServers:Cache:Remote:Templates");
            ObjectMap<String> templates = new ObjectMap<>((Map<String, ?>) data.getObject(0x0000));
            map.clear();
            for (String name : templates.getKeys()) {
                try {
                    UniversalFile dir = new UniversalFile(templatedir, name);
                    SubCreator.ServerTemplate template = new SubCreator.ServerTemplate(name, templates.getMap(name).getBoolean("enabled"), templates.getMap(name).getRawString("icon"), dir,
                            templates.getMap(name).getMap("build").clone(), templates.getMap(name).getMap("settings").clone());
                    map.put(name.toLowerCase(), template);
                    if (!templates.getMap(name).getRawString("display").equals(name)) template.setDisplayName(templates.getMap(name).getRawString("display"));
                } catch (Exception e) {
                    Logger.getLogger("SubServers").severe("Couldn't load template: " + name);
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
