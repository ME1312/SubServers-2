package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Event.*;

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
     * New PacketInRunEvent
     */
    public PacketInExRunEvent() {
        callback("SubAddHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubAddHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubAddProxyEvent(data.getRawString("proxy")));
                callback("SubAddProxyEvent", this);
            }
        });
        callback("SubAddServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubAddServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getRawString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubCreateEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host"), data.getRawString("name"),
                        data.getRawString("template"), data.getVersion("version"), data.getInt("port"), data.getBoolean("update")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubSendCommandEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubSendCommandEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), data.getRawString("command")));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubEditServerEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), new NamedContainer<String, Object>(data.getRawString("edit"), data.get("value")), data.getBoolean("perm")));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubStartEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStopEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubStopEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("server"), data.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubStoppedEvent(data.getRawString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubRemoveServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getRawString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubAddProxyEvent(data.getRawString("proxy")));
                callback("SubRemoveProxyEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubRemoveHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getRawString("host")));
                callback("SubRemoveHostEvent", this);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) {
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
