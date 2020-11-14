package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.*;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Bungee.Library.SubEvent;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubServers.Bungee.SubProxy;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Event Send Packet
 */
public class PacketOutExRunEvent implements Listener, PacketObjectOut<Integer> {
    private SubProxy plugin;
    private ObjectMap<String> args;
    private String type;

    /**
     * New PacketOutExRunEvent (Registerer)
     *
     * @param plugin
     */
    public PacketOutExRunEvent(SubProxy plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketOutExRunEvent (Out)
     *
     * @param event Event to be run
     * @param args Arguments
     */
    public PacketOutExRunEvent(Class<? extends SubEvent> event, ObjectMap<String> args) {
        if (Util.isNull(event, args)) throw new NullPointerException();
        this.type = event.getSimpleName();
        this.args = args;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) {
        ObjectMap<Integer> json = new ObjectMap<Integer>();
        json.set(0x0000, type);
        json.set(0x0001, args);
        return json;
    }

    @Override
    public int version() {
        return 0x0001;
    }

    private void broadcast(PacketOutExRunEvent packet) {
        broadcast(null, packet);
    }

    private void broadcast(Object self, PacketOutExRunEvent packet) {
        if (plugin.subdata != null) {
            List<SubDataClient> clients = new LinkedList<SubDataClient>();
            clients.addAll(plugin.subdata.getClients().values());
            for (SubDataClient client : clients) {
                if (client.getHandler() == null || client.getHandler() != self) { // Don't send events about yourself to yourself
                    if (client.getHandler() == null || client.getHandler().getSubData()[0] == client) { // Don't send events over subchannels
                        client.sendPacket(packet);
                    }
                }
            }
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubAddProxyEvent event) {
        ObjectMap<String> args = new ObjectMap<String>();
        args.set("proxy", event.getProxy().getName());
        broadcast(event.getProxy(), new PacketOutExRunEvent(event.getClass(), args));
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubAddHostEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("host", event.getHost().getName());
            broadcast(event.getHost(), new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubAddServerEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            if (event.getHost() != null) args.set("host", event.getHost().getName());
            args.set("server", event.getServer().getName());
            broadcast(event.getServer(), new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubCreateEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("update", event.isUpdate());
            args.set("name", event.getName());
            args.set("host", event.getHost().getName());
            args.set("template", event.getTemplate().getName());
            if (event.getVersion() != null) args.set("version", event.getVersion());
            args.set("port", event.getPort());
            broadcast(new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubCreatedEvent event) {
        ObjectMap<String> args = new ObjectMap<String>();
        if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
        args.set("success", event.wasSuccessful());
        args.set("update", event.wasUpdate());
        args.set("name", event.getName());
        args.set("host", event.getHost().getName());
        args.set("template", event.getTemplate().getName());
        if (event.getVersion() != null) args.set("version", event.getVersion());
        args.set("port", event.getPort());
        broadcast(new PacketOutExRunEvent(event.getClass(), args));
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubSendCommandEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("server", event.getServer().getName());
            args.set("command", event.getCommand());
            broadcast(new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubEditServerEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("server", event.getServer().getName());
            args.set("edit", event.getEdit().key());
            args.set("value", event.getEdit().value().asObject());
            args.set("perm", event.isPermanent());
            broadcast(new PacketOutExRunEvent(event.getClass(), args));
        }
    }
    
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStartEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("server", event.getServer().getName());
            broadcast(new PacketOutExRunEvent(event.getClass(), args));
        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStartedEvent event) {
        ObjectMap<String> args = new ObjectMap<String>();
        args.set("server", event.getServer().getName());
        broadcast(event.getServer(), new PacketOutExRunEvent(event.getClass(), args));

    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStopEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("server", event.getServer().getName());
            args.set("force", event.isForced());
            broadcast(new PacketOutExRunEvent(event.getClass(), args));

        }
    }
    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubStoppedEvent event) {
        ObjectMap<String> args = new ObjectMap<String>();
        args.set("server", event.getServer().getName());
        broadcast(event.getServer(), new PacketOutExRunEvent(event.getClass(), args));

    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubRemoveServerEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            if (event.getHost() != null) args.set("host", event.getHost().getName());
            args.set("server", event.getServer().getName());
            broadcast(event.getServer(), new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubRemoveHostEvent event) {
        if (!event.isCancelled()) {
            ObjectMap<String> args = new ObjectMap<String>();
            if (event.getPlayer() != null) args.set("player", event.getPlayer().toString());
            args.set("host", event.getHost().getName());
            broadcast(event.getHost(), new PacketOutExRunEvent(event.getClass(), args));
        }
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void event(SubRemoveProxyEvent event) {
        ObjectMap<String> args = new ObjectMap<String>();
        args.set("proxy", event.getProxy().getName());
        broadcast(event.getProxy(), new PacketOutExRunEvent(event.getClass(), args));
    }
}