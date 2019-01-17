package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Event.*;
import net.ME1312.SubServers.Client.Sponge.Library.Callback;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.NamedContainer;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.spongepowered.api.Sponge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Run Event Packet
 */
public class PacketInRunEvent implements PacketIn {
    private static HashMap<String, List<Callback<YAMLSection>>> callbacks = new HashMap<String, List<Callback<YAMLSection>>>();

    /**
     * New PacketInRunEvent
     */
    public PacketInRunEvent(SubPlugin plugin) {
        callback("SubAddHostEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubAddHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
                callback("SubAddHostEvent", this);
            }
        });
        callback("SubAddProxyEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubAddProxyEvent(data.getString("proxy")));
                callback("SubAddProxyEvent", this);
            }
        });
        callback("SubAddServerEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubAddServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getString("server")));
                callback("SubAddServerEvent", this);
            }
        });
        callback("SubCreateEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubCreateEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host"), data.getString("name"),
                        data.getString("template"), new Version(data.getString("version")), data.getInt("port")));
                callback("SubCreateEvent", this);
            }
        });
        callback("SubSendCommandEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubSendCommandEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getString("command")));
                callback("SubSendCommandEvent", this);
            }
        });
        callback("SubEditServerEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubEditServerEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), new NamedContainer<String, Object>(data.getString("edit"), data.get("value")), data.getBoolean("perm")));
                callback("SubEditServerEvent", this);
            }
        });
        callback("SubStartEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubStartEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server")));
                callback("SubStartEvent", this);
            }
        });
        callback("SubStopEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubStopEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("server"), data.getBoolean("force")));
                callback("SubStopEvent", this);
            }
        });
        callback("SubStoppedEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubStoppedEvent(data.getString("server")));
                callback("SubStoppedEvent", this);
            }
        });
        callback("SubRemoveServerEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubRemoveServerEvent((data.contains("player"))?data.getUUID("player"):null, (data.contains("host"))?data.getRawString("host"):null, data.getString("server")));
                callback("SubRemoveServerEvent", this);
            }
        });
        callback("SubRemoveProxyEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubRemoveProxyEvent(data.getString("proxy")));
                callback("SubRemoveProxyEvent", this);
            }
        });
        callback("SubRemoveHostEvent", new Callback<YAMLSection>() {
            @Override
            public void run(YAMLSection data) {
                Sponge.getEventManager().post(new SubRemoveHostEvent((data.contains("player"))?data.getUUID("player"):null, data.getString("host")));
                callback("SubRemoveHostEvent", this);
            }
        });
    }

    @Override
    public void execute(YAMLSection data) {
        if (callbacks.keySet().contains(data.getString("type"))) {
            List<Callback<YAMLSection>> callbacks = PacketInRunEvent.callbacks.get(data.getString("type"));
            PacketInRunEvent.callbacks.remove(data.getString("type"));
            for (Callback callback : callbacks) {
                callback.run(data.getSection("args"));
            }
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }

    public static void callback(String event, Callback<YAMLSection> callback) {
        List<Callback<YAMLSection>> callbacks = (PacketInRunEvent.callbacks.keySet().contains(event))?PacketInRunEvent.callbacks.get(event):new ArrayList<Callback<YAMLSection>>();
        callbacks.add(callback);
        PacketInRunEvent.callbacks.put(event, callbacks);
    }
}
