package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event Send Packet
 */
public class PacketOutRunEvent implements Listener, PacketOut {
    private SubPlugin plugin;
    private JSONObject args;
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
    public PacketOutRunEvent(Class<? extends SubEvent> event, JSONObject args) {
        if (Util.isNull(event, args)) throw new NullPointerException();
        this.type = event.getSimpleName();
        this.args = args;
    }

    @Override
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("args", args);
        return json;
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubAddServerEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("host", ((event.getHost() == null)?null:event.getHost()));
            args.put("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubCreateEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("host", event.getHost().getName());
            args.put("name", event.getName());
            args.put("template", event.getTemplate().toString());
            args.put("version", event.getVersion().toString());
            args.put("port", event.getPort());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubSendCommandEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("server", event.getServer().getName());
            args.put("command", event.getCommand());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStartEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStopEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("server", event.getServer().getName());
            args.put("force", event.isForced());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));

        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStoppedEvent event) {
        JSONObject args = new JSONObject();
        args.put("server", event.getServer().getName());
        plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));

    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubRemoveServerEvent event) {
        if (!event.isCancelled()) {
            JSONObject args = new JSONObject();
            args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
            args.put("host", ((event.getHost() == null)?null:event.getHost()));
            args.put("server", event.getServer().getName());
            plugin.subdata.broadcastPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
}