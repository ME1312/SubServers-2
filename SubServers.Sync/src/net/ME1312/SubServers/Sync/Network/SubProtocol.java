package net.ME1312.SubServers.Sync.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Sync.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Sync.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Sync.Network.API.Server;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.SubAPI;
import net.ME1312.SubServers.Sync.SubPlugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.conf.Configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private SubProtocol() {}

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            instance = new SubProtocol();
            SubPlugin plugin = SubAPI.getInstance().getInternals();

            instance.setName("SubServers 2");
            instance.addVersion(new Version("2.14a+"));


            // 00-0F: Object Link Packets
            instance.registerPacket(0x0000, PacketLinkProxy.class);
            instance.registerPacket(0x0000, new PacketLinkProxy(plugin));


            // 10-2F: Download Packets
            instance.registerPacket(0x0010, PacketDownloadLang.class);
            instance.registerPacket(0x0011, PacketDownloadPlatformInfo.class);
            instance.registerPacket(0x0012, PacketDownloadProxyInfo.class);
            instance.registerPacket(0x0013, PacketDownloadHostInfo.class);
            instance.registerPacket(0x0014, PacketDownloadGroupInfo.class);
            instance.registerPacket(0x0015, PacketDownloadServerInfo.class);
            instance.registerPacket(0x0016, PacketDownloadPlayerList.class);
            instance.registerPacket(0x0017, PacketCheckPermission.class);

            instance.registerPacket(0x0010, new PacketDownloadLang(plugin));
            instance.registerPacket(0x0011, new PacketDownloadPlatformInfo());
            instance.registerPacket(0x0012, new PacketDownloadProxyInfo());
            instance.registerPacket(0x0013, new PacketDownloadHostInfo());
            instance.registerPacket(0x0014, new PacketDownloadGroupInfo());
            instance.registerPacket(0x0015, new PacketDownloadServerInfo());
            instance.registerPacket(0x0016, new PacketDownloadPlayerList());
            instance.registerPacket(0x0017, new PacketCheckPermission());


            // 30-4F: Control Packets
            instance.registerPacket(0x0030, PacketCreateServer.class);
            instance.registerPacket(0x0031, PacketAddServer.class);
            instance.registerPacket(0x0032, PacketStartServer.class);
            instance.registerPacket(0x0033, PacketUpdateServer.class);
            instance.registerPacket(0x0034, PacketEditServer.class);
            instance.registerPacket(0x0035, PacketRestartServer.class);
            instance.registerPacket(0x0036, PacketCommandServer.class);
            instance.registerPacket(0x0037, PacketStopServer.class);
            instance.registerPacket(0x0038, PacketRemoveServer.class);
            instance.registerPacket(0x0039, PacketDeleteServer.class);

            instance.registerPacket(0x0030, new PacketCreateServer());
            instance.registerPacket(0x0031, new PacketAddServer());
            instance.registerPacket(0x0032, new PacketStartServer());
            instance.registerPacket(0x0033, new PacketUpdateServer());
            instance.registerPacket(0x0034, new PacketEditServer());
            instance.registerPacket(0x0035, new PacketRestartServer());
            instance.registerPacket(0x0036, new PacketCommandServer());
            instance.registerPacket(0x0037, new PacketStopServer());
            instance.registerPacket(0x0038, new PacketRemoveServer());
            instance.registerPacket(0x0039, new PacketDeleteServer());


            // 70-7F: External Misc Packets
          //instance.registerPacket(0x0070, PacketInExRunEvent.class);
          //instance.registerPacket(0x0071, PacketInExReset.class);
          //instance.registerPacket(0x0073, PacketInExReload.class);

            instance.registerPacket(0x0070, new PacketInExRunEvent(plugin));
            instance.registerPacket(0x0071, new PacketInExReset());
            instance.registerPacket(0x0073, new PacketInExUpdateWhitelist(plugin));
        }

        return instance;
    }

    private Logger getLogger(int channel) {
        return net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubData" + ((channel != 0)? "/Sub-"+channel:""));
    }

    @Override
    protected SubDataClient sub(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("subdata"), plugin), null);

        int channel = 1;
        while (map.keySet().contains(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkProxy(plugin, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        SubDataClient subdata = super.open(scheduler, logger, address, port);
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("subdata"), plugin), null);

        subdata.sendPacket(new PacketLinkProxy(plugin, 0));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.sendPacket(new PacketDownloadPlatformInfo(platform -> {
            if (plugin.lastReload != platform.getMap("subservers").getLong("last-reload")) {
                net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubServers").info("Resetting Server Data");
                plugin.servers.clear();
                plugin.lastReload = platform.getMap("subservers").getLong("last-reload");
            }
            try {
                LinkedList<ListenerInfo> listeners = new LinkedList<ListenerInfo>(plugin.getConfig().getListeners());
                for (int i = 0; i < platform.getMap("bungee").getMapList("listeners").size(); i++) if (i < listeners.size()) {
                    if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Forced-Hosts", true)) Util.reflect(ListenerInfo.class.getDeclaredField("forcedHosts"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getMap("forced-hosts").get());
                    if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Motd", false)) Util.reflect(ListenerInfo.class.getDeclaredField("motd"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getRawString("motd"));
                    if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Player-Limit", false)) Util.reflect(ListenerInfo.class.getDeclaredField("maxPlayers"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getInt("player-limit"));
                    if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Server-Priorities", true)) Util.reflect(ListenerInfo.class.getDeclaredField("serverPriority"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getRawStringList("priorities"));
                }
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Disabled-Commands", false)) Util.reflect(Configuration.class.getDeclaredField("disabledCommands"), plugin.getConfig(), platform.getMap("bungee").getRawStringList("disabled-cmds"));
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Player-Limit", false)) Util.reflect(Configuration.class.getDeclaredField("playerLimit"), plugin.getConfig(), platform.getMap("bungee").getInt("player-limit"));
            } catch (Exception e) {
                net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubServers").info("Problem syncing BungeeCord configuration options");
                e.printStackTrace();
            }

            plugin.api.getServers(servers -> {
                for (Server server : servers.values()) {
                    plugin.merge(server);
                }
            });
        }));
        subdata.on.ready(client -> plugin.getPluginManager().callEvent(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            plugin.getPluginManager().callEvent(event);
            map.put(0, null);

            int reconnect = plugin.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 30);
            if (Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("reconnect"), plugin), false) && reconnect > 0
                    && client.name() != DisconnectReason.PROTOCOL_MISMATCH && client.name() != DisconnectReason.ENCRYPTION_MISMATCH) {
                net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubData").info("Attempting reconnect in " + reconnect + " seconds");
                Timer timer = new Timer("SubServers.Sync::SubData_Reconnect_Handler");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Util.reflect(SubPlugin.class.getDeclaredMethod("connect"), plugin);
                            timer.cancel();
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof IOException) {
                                net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubData").info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                            } else e.printStackTrace();
                        } catch (NoSuchMethodException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
            }
        });

        return subdata;
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
