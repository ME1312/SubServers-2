package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Host.External.ExternalHost;
import net.ME1312.SubServers.Bungee.Host.External.ExternalSubCreator;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * External Host Template Upload Packet
 */
public class PacketExUploadTemplates implements PacketObjectIn<Integer>, PacketOut {
    private static LinkedList<Runnable> callbacks = new LinkedList<Runnable>();
    private SubProxy plugin;

    /**
     * New PacketExUploadTemplates
     */
    public PacketExUploadTemplates(SubProxy plugin, Runnable... callbacks) {
        this.plugin = plugin;
        PacketExUploadTemplates.callbacks.addAll(Arrays.asList(callbacks));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
        if (client.getHandler() != null && client.getHandler() instanceof ExternalHost) {
            HashMap<String, SubCreator.ServerTemplate> map = Try.all.get(() -> Util.reflect(ExternalSubCreator.class.getDeclaredField("templates"), ((ExternalHost) client.getHandler()).getCreator()), new HashMap<>());
            File templatedir = new File(plugin.dir, "SubServers/Cache/Remote/Templates");
            ObjectMap<String> templates = new ObjectMap<>((Map<String, ?>) data.getObject(0x0000));
            map.clear();
            for (String name : templates.getKeys()) {
                try {
                    File dir = new File(templatedir, name);
                    SubCreator.ServerTemplate template = Util.reflect(SubCreator.class.getDeclaredMethod("loadTemplate", String.class, boolean.class, boolean.class, String.class, File.class, ObjectMap.class, ObjectMap.class),
                            ((ExternalHost) client.getHandler()).getCreator(), name, templates.getMap(name).getBoolean("enabled"), templates.getMap(name).getBoolean("internal"), templates.getMap(name).getString("icon"), dir,
                            templates.getMap(name).getMap("build").clone(), templates.getMap(name).getMap("settings").clone());
                    map.put(name.toLowerCase(), template);
                    if (!templates.getMap(name).getString("display").equals(name)) template.setDisplayName(templates.getMap(name).getString("display"));
                } catch (Exception e) {
                    Logger.getLogger("SubServers").severe("Couldn't load template: " + name);
                    e.printStackTrace();
                }
            }

            LinkedList<Runnable> callbacks = PacketExUploadTemplates.callbacks;
            PacketExUploadTemplates.callbacks = new LinkedList<Runnable>();
            for (Runnable r : callbacks) {
                r.run();
            }
        }
    }

    @Override
    public int version() {
        return 0x0002;
    }
}
