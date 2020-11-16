package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.SubDataSender;
import net.ME1312.SubServers.Client.Sponge.Event.*;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;

import org.spongepowered.api.Sponge;

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
    public PacketInExRunEvent(SubPlugin plugin) {
        callback("SubAddHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubAddHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubAddProxyEvent(data.getString("proxy")));
                callback("SubAddProxyEvent", this);
            }
        });
        callback("SubAddServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubAddServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubCreateEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host"), data.getString("name"),
                        data.getString("template"), new Version(data.getString("version")), data.getInt("port"), data.getBoolean("update")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubCreatedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubCreatedEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host"), data.getString("name"),
                        data.getString("template"), new Version(data.getString("version")), data.getInt("port"), data.getBoolean("update"), data.getBoolean("success")));
                callback("SubCreatedEvent", this);
            }
        });
        callback("SubSendCommandEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubSendCommandEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getString("command")));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubEditServerEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), new ContainedPair<String, Object>(data.getString("edit"), data.get("value")), data.getBoolean("perm")));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubStartEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStartedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubStartedEvent(data.getString("server")));
                callback("SubStartedEvent", this);
            }
        });
        callback("SubStopEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubStopEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubStoppedEvent(data.getString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubRemoveServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveProxyEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubRemoveProxyEvent(data.getString("proxy")));
                callback("SubRemoveProxyEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new Callback<ObjectMap<String>>() {
            @Override
            public void run(ObjectMap<String> data) {
                Sponge.getEventManager().post(new SubRemoveHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
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
