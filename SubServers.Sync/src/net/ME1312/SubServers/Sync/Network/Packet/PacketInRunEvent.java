package net.ME1312.SubServers.Sync.Network.Packet;

import net.ME1312.SubServers.Sync.Event.*;
import net.ME1312.SubServers.Sync.Library.JSONCallback;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.PacketIn;
import net.md_5.bungee.api.ProxyServer;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Run Event Packet
 */
public class PacketInRunEvent implements PacketIn {
    private static HashMap<String, List<JSONCallback>> callbacks = new HashMap<String, List<JSONCallback>>();

    /**
     * New PacketInRunEvent
     */
    public PacketInRunEvent() {
        callback("SubAddHostEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddHostEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddServerEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubAddServerEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("host"), json.getString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubCreateEvent((json.keySet().contains("player")) ? UUID.fromString(json.getString("player")) : null, json.getString("host"), json.getString("name"),
                        json.getString("template"), new Version(json.getString("version")), json.getInt("port")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubSendCommandEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubSendCommandEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("server"), json.getString("command")));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubEditServerEvent((json.keySet().contains("player")) ? UUID.fromString(json.getString("player")) : null, json.getString("server"), new NamedContainer<String, Object>(json.getString("edit"), json.get("value")), json.getBoolean("perm")));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStartEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStopEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStopEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("server"), json.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubStoppedEvent(json.getString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveServerEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("host"), json.getString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new JSONCallback() {
            @Override
            public void run(JSONObject json) {
                ProxyServer.getInstance().getPluginManager().callEvent(new SubRemoveHostEvent((json.keySet().contains("player"))?UUID.fromString(json.getString("player")):null, json.getString("host")));
                callback("SubRemoveHostEvent", this);
            }
        });
    }

    @Override
    public void execute(JSONObject data) {
        if (callbacks.keySet().contains(data.getString("type"))) {
            List<JSONCallback> callbacks = PacketInRunEvent.callbacks.get(data.getString("type"));
            PacketInRunEvent.callbacks.remove(data.getString("type"));
            for (JSONCallback callback : callbacks) {
                callback.run(data.getJSONObject("args"));
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }

    public static void callback(String event, JSONCallback callback) {
        List<JSONCallback> callbacks = (PacketInRunEvent.callbacks.keySet().contains(event))?PacketInRunEvent.callbacks.get(event):new ArrayList<JSONCallback>();
        callbacks.add(callback);
        PacketInRunEvent.callbacks.put(event, callbacks);
    }
}
