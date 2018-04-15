package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Event Send Packet
 */
public class PacketOutRunEvent implements Listener, PacketOut {
    private SubPlugin plugin;
    private YAMLSection args;
    private String type;

    /**
     * New PacketOutRunEvent (Registerer)
     *
     * @param plugin
     */
    public PacketOutRunEvent(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketOutRunEvent (Out)
     *
     * @param event Event to be run
     * @param args Arguments
     */
    public PacketOutRunEvent(Class<? extends SubEvent> event, YAMLSection args) {
        if (Util.isNull(event, args)) throw new NullPointerException();
        this.type = event.getSimpleName();
        this.args = args;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection json = new YAMLSection();
        json.set("type", type);
        json.set("args", args);
        return json;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubAddHostEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("host", event.getHost().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubAddServerEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("host", ((event.getHost() == null)?null:event.getHost().getName()));
            args.set("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubCreateEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("host", event.getHost().getName());
            args.set("name", event.getName());
            args.set("template", event.getTemplate().getName());
            args.set("version", event.getVersion().toString());
            args.set("port", event.getPort());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubSendCommandEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("server", event.getServer().getName());
            args.set("command", event.getCommand());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubEditServerEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("server", event.getServer().getName());
            args.set("edit", event.getEdit().name());
            args.set("value", event.getEdit().get().asObject());
            args.set("perm", event.isPermanent());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStartEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStopEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("server", event.getServer().getName());
            args.set("force", event.isForced());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));

        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStoppedEvent event) {
        YAMLSection args = new YAMLSection();
        args.set("server", event.getServer().getName());
        plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));

    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubRemoveServerEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("host", ((event.getHost() == null)?null:event.getHost().getName()));
            args.set("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubRemoveHostEvent event) {
        if (!event.isCancelled()) {
            YAMLSection args = new YAMLSection();
            args.set("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.set("host", event.getHost().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
}