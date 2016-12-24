package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
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

public class PacketOutRunEvent implements Listener, PacketOut {
    private SubPlugin plugin;
    private Map<String, ?> args;
    private String type;

    public PacketOutRunEvent(SubPlugin plugin) {
        this.plugin = plugin;
    }

    public PacketOutRunEvent(Class<? extends SubEvent> event, Map<String, ?> args) {
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
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("host", event.getHost().getName());
                args.put("server", event.getServer().getName());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubCreateEvent event) {
        if (!event.isCancelled()) {
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("host", event.getHost().getName());
                args.put("name", event.getName());
                args.put("type", event.getType().toString());
                args.put("version", event.getVersion().toString());
                args.put("port", event.getPort());
                args.put("memory", event.getMemory());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubSendCommandEvent event) {
        if (!event.isCancelled()) {
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("server", event.getServer().getName());
                args.put("command", event.getCommand());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStartEvent event) {
        if (!event.isCancelled()) {
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("server", event.getServer().getName());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStopEvent event) {
        if (!event.isCancelled()) {
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("server", event.getServer().getName());
                args.put("force", event.isForced());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubStoppedEvent event) {
        List<Server> list = new ArrayList<Server>();
        list.addAll(plugin.api.getServers().values());
        for (Server server : list) {
            HashMap<String, Object> args = new HashMap<String, Object>();
            args.put("server", event.getServer().getName());
            if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void event(SubRemoveServerEvent event) {
        if (!event.isCancelled()) {
            List<Server> list = new ArrayList<Server>();
            list.addAll(plugin.api.getServers().values());
            for (Server server : list) {
                HashMap<String, Object> args = new HashMap<String, Object>();
                args.put("player", ((event.getPlayer() == null)?null:event.getPlayer().toString()));
                args.put("host", event.getHost().getName());
                args.put("server", event.getServer().getName());
                if (server.getSubDataClient() != null) server.getSubDataClient().sendPacket(new PacketOutRunEvent(event.getClass(), args));
            }
        }
    }
}