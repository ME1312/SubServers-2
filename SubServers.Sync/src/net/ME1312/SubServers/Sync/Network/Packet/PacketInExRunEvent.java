package net.ME1312.SubServers.Sync.Network.Packet;

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
import java.util.function.Consumer;

/**
 * Run Event Packet
 */
public class PacketInExRunEvent implements PacketObjectIn<Integer> {
    private static HashMap<String, List<Consumer<ObjectMap<String>>>> callbacks = new HashMap<String, List<Consumer<ObjectMap<String>>>>();

    /**
     * New PacketInExRunEvent
     */
    public PacketInExRunEvent(ExProxy plugin) {
        callback("SubAddHostEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddProxyEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddProxyEvent(data.getString("proxy")));
                callback("SubAddProxyEvent", this);
            }
        });
        callback("SubAddServerEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getString("host"):null, data.getString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubCreateEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host"), data.getString("name"),
                        data.getString("template"), data.getVersion("version"), data.getInt("port"), data.getBoolean("update")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubCreatedEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubCreatedEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host"), data.getString("name"),
                        data.getString("template"), data.getVersion("version"), data.getInt("port"), data.getBoolean("update"), data.getBoolean("success")));
                callback("SubCreatedEvent", this);
            }
        });
        callback("SubSendCommandEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubSendCommandEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getString("command"), (data.contains("target"))?data.getUUID("target"):null));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubEditServerEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), new ContainedPair<String, Object>(data.getString("edit"), data.get("value"))));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStartEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStartedEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStartedEvent(data.getString("server")));
                callback("SubStartedEvent", this);
            }
        });
        callback("SubStopEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStopEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStoppedEvent(data.getString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getString("host"):null, data.getString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveProxyEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddProxyEvent(data.getString("proxy")));
                callback("SubRemoveProxyEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new Consumer<ObjectMap<String>>() {
            @Override
            public void accept(ObjectMap<String> data) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
                callback("SubRemoveHostEvent", this);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender client, ObjectMap<Integer> data) {
        if (callbacks.keySet().contains(data.getString(0x0000))) {
            for (Consumer<ObjectMap<String>> callback : PacketInExRunEvent.callbacks.remove(data.getString(0x0000))) {
                callback.accept(new ObjectMap<>((Map<String, ?>) data.getObject(0x0001)));
            }
        }
    }

    public static void callback(String event, Consumer<ObjectMap<String>> callback) {
        List<Consumer<ObjectMap<String>>> callbacks = (PacketInExRunEvent.callbacks.keySet().contains(event))? PacketInExRunEvent.callbacks.get(event):new ArrayList<Consumer<ObjectMap<String>>>();
        callbacks.add(callback);
        PacketInExRunEvent.callbacks.put(event, callbacks);
    }
}
