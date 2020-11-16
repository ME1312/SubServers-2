package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.ExProxy;

import net.md_5.bungee.api.ProxyServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Run Event Packet
 */
public class PacketInExRunEvent implements PacketObjectIn<Integer> {
    private static HashMap<String, List<Callback<ObjectMap<String>>>> callbacks = new HashMap<String, List<Callback<ObjectMap<String>>>>();

    /**
     * New PacketInExRunEvent
     */
    public PacketInExRunEvent(ExProxy plugin) {
        callback("SubAddHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddProxyEvent(data.getRawString("proxy")));
                callback("SubAddProxyEvent", this);
            }
        });
        callback("SubAddServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getRawString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubCreateEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host"), data.getRawString("name"),
                        data.getRawString("template"), data.getVersion("version"), data.getInt("port"), data.getBoolean("update")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubCreatedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubCreatedEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host"), data.getRawString("name"),
                        data.getRawString("template"), data.getVersion("version"), data.getInt("port"), data.getBoolean("update"), data.getBoolean("success")));
                callback("SubCreatedEvent", this);
            }
        });
        callback("SubSendCommandEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubSendCommandEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), data.getRawString("command")));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubEditServerEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), new ContainedPair<String, Object>(data.getRawString("edit"), data.get("value")), data.getBoolean("perm")));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStartEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStartedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStartedEvent(data.getRawString("server")));
                callback("SubStartedEvent", this);
            }
        });
        callback("SubNetworkConnectEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                plugin.connect(plugin.servers.get(data.getRawString("server").toLowerCase()), data.getInt("channel"), data.getUUID("id"));
                callback("SubNetworkConnectEvent", this);
            }
        });
        callback("SubNetworkDisconnectEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                plugin.disconnect(plugin.servers.get(data.getRawString("server").toLowerCase()), data.getInt("channel"));
                callback("SubNetworkDisconnectEvent", this);
            }
        });
        callback("SubStopEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStopEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), data.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStoppedEvent(data.getRawString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getRawString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddProxyEvent(data.getRawString("proxy")));
                callback("SubRemoveProxyEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host")));
                callback("SubRemoveHostEvent", this);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        if (callbacks.keySet().contains(data.getString(0x0000))) {
            List<Callback<ObjectMap<String>>> callbacks = PacketInExRunEvent.callbacks.get(data.getString(0x0000));
            PacketInExRunEvent.callbacks.remove(data.getString(0x0000));
            for (Callback<ObjectMap<String>> callback : callbacks) {
                callback.run(new ObjectMap<>((Map<String, ?>) data.getObject(0x0001)));
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }

    public static void callback(String event, Callback<ObjectMap<String>> callback) {
        List<Callback<ObjectMap<String>>> callbacks = (PacketInExRunEvent.callbacks.keySet().contains(event))? PacketInExRunEvent.callbacks.get(event):new ArrayList<Callback<ObjectMap<String>>>();
        callbacks.add(callback);
        PacketInExRunEvent.callbacks.put(event, callbacks);
    }
}
