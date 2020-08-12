package net.ME1312.SubServers.Sync.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Sync.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Sync.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Sync.ExProxy;
import net.ME1312.SubServers.Sync.Network.API.RemotePlayer;
import net.ME1312.SubServers.Sync.Network.API.Server;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.Server.ServerImpl;
import net.ME1312.SubServers.Sync.SubAPI;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.conf.Configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * SubServers Protocol Class
 */
public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;

    @SuppressWarnings("deprecation")
    protected SubProtocol() {
        ExProxy plugin = SubAPI.getInstance().getInternals();

        setName("SubServers 2");
        addVersion(new Version("2.16a+"));


        // 00-0F: Object Link Packets
        registerPacket(0x0000, PacketLinkProxy.class);
        registerPacket(0x0000, new PacketLinkProxy(plugin));


        // 10-2F: Download Packets
        registerPacket(0x0010, PacketDownloadLang.class);
        registerPacket(0x0011, PacketDownloadPlatformInfo.class);
        registerPacket(0x0012, PacketDownloadProxyInfo.class);
        registerPacket(0x0013, PacketDownloadHostInfo.class);
        registerPacket(0x0014, PacketDownloadGroupInfo.class);
        registerPacket(0x0015, PacketDownloadServerInfo.class);
        registerPacket(0x0016, PacketDownloadPlayerInfo.class);
        registerPacket(0x0017, PacketCheckPermission.class);
        registerPacket(0x0018, PacketCheckPermissionResponse.class);

        registerPacket(0x0010, new PacketDownloadLang(plugin));
        registerPacket(0x0011, new PacketDownloadPlatformInfo());
        registerPacket(0x0012, new PacketDownloadProxyInfo());
        registerPacket(0x0013, new PacketDownloadHostInfo());
        registerPacket(0x0014, new PacketDownloadGroupInfo());
        registerPacket(0x0015, new PacketDownloadServerInfo());
        registerPacket(0x0016, new PacketDownloadPlayerInfo());
        registerPacket(0x0017, new PacketCheckPermission());
        registerPacket(0x0018, new PacketCheckPermissionResponse());


        // 30-4F: Control Packets
        registerPacket(0x0030, PacketCreateServer.class);
        registerPacket(0x0031, PacketAddServer.class);
        registerPacket(0x0032, PacketStartServer.class);
        registerPacket(0x0033, PacketUpdateServer.class);
        registerPacket(0x0034, PacketEditServer.class);
        registerPacket(0x0035, PacketRestartServer.class);
        registerPacket(0x0036, PacketCommandServer.class);
        registerPacket(0x0037, PacketStopServer.class);
        registerPacket(0x0038, PacketRemoveServer.class);
        registerPacket(0x0039, PacketDeleteServer.class);

        registerPacket(0x0030, new PacketCreateServer());
        registerPacket(0x0031, new PacketAddServer());
        registerPacket(0x0032, new PacketStartServer());
        registerPacket(0x0033, new PacketUpdateServer());
        registerPacket(0x0034, new PacketEditServer());
        registerPacket(0x0035, new PacketRestartServer());
        registerPacket(0x0036, new PacketCommandServer());
        registerPacket(0x0037, new PacketStopServer());
        registerPacket(0x0038, new PacketRemoveServer());
        registerPacket(0x0039, new PacketDeleteServer());


        // 70-7F: External Misc Packets
      //registerPacket(0x0070, PacketInExRunEvent.class);
      //registerPacket(0x0071, PacketInExReset.class);
      //registerPacket(0x0073, PacketInExReload.class);
        registerPacket(0x0074, PacketExSyncPlayer.class);

        registerPacket(0x0070, new PacketInExRunEvent(plugin));
        registerPacket(0x0071, new PacketInExReset());
        registerPacket(0x0073, new PacketInExUpdateWhitelist(plugin));
        registerPacket(0x0074, new PacketExSyncPlayer(plugin));
    }

    public static SubProtocol get() {
        if (instance == null)
            instance = new SubProtocol();

        return instance;
    }

    private Logger getLogger(int channel) {
        return net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubData" + ((channel != 0)? "/Sub-"+channel:""));
    }

    @Override
    protected SubDataClient sub(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        ExProxy plugin = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(ExProxy.class.getDeclaredField("subdata"), plugin), null);

        int channel = 1;
        while (map.keySet().contains(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port, login);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkProxy(plugin, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        ExProxy plugin = SubAPI.getInstance().getInternals();
        SubDataClient subdata = super.open(scheduler, logger, address, port);
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(ExProxy.class.getDeclaredField("subdata"), plugin), null);

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

            ArrayList<RemotePlayer> localPlayers = new ArrayList<RemotePlayer>();
            for (UUID id : new ArrayList<UUID>(plugin.rPlayers.keySet())) {
                if (plugin.getPlayer(id) != null) {
                    localPlayers.add(plugin.rPlayers.get(id));
                } else {
                    plugin.rPlayerLinkS.remove(id);
                    plugin.rPlayerLinkP.remove(id);
                    plugin.rPlayers.remove(id);
                }
            }
            subdata.sendPacket(new PacketExSyncPlayer(null, localPlayers.toArray(new RemotePlayer[0])));

            plugin.api.getServers(servers -> {
                for (Server server : servers.values()) {
                    plugin.merge(server);
                }

                plugin.api.getGlobalPlayers(players -> {
                    for (RemotePlayer player : players.values()) {
                        plugin.rPlayers.put(player.getUniqueId(), player);
                        plugin.rPlayerLinkP.put(player.getUniqueId(), player.getProxy().toLowerCase());

                        ServerInfo server = plugin.getServerInfo(player.getServer());
                        if (server instanceof ServerImpl)
                            plugin.rPlayerLinkS.put(player.getUniqueId(), (ServerImpl) server);
                    }
                });
            });

        }));
        subdata.on.ready(client -> plugin.getPluginManager().callEvent(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            plugin.getPluginManager().callEvent(event);

            if (plugin.isRunning) {
                net.ME1312.SubServers.Sync.Library.Compatibility.Logger.get("SubData").info("Attempting reconnect in " + plugin.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 30) + " seconds");
                Util.isException(() -> Util.reflect(ExProxy.class.getDeclaredMethod("connect", NamedContainer.class), plugin, client));
            } else map.put(0, null);
        });

        return subdata;
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
