package net.ME1312.SubServers.Velocity.Network;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Common.Network.API.RemotePlayer;
import net.ME1312.SubServers.Client.Common.Network.API.Server;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;
import net.ME1312.SubServers.Velocity.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Velocity.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Velocity.Event.SubRemoveServerEvent;
import net.ME1312.SubServers.Velocity.ExProxy;
import net.ME1312.SubServers.Velocity.Network.Packet.*;
import net.ME1312.SubServers.Velocity.Server.CachedPlayer;
import net.ME1312.SubServers.Velocity.Server.ServerData;
import net.ME1312.SubServers.Velocity.SubAPI;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
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
        addVersion(new Version("2.18a+"));


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
        registerPacket(0x003B, PacketTransferPlayer.class);
        registerPacket(0x003C, PacketDisconnectPlayer.class);
        registerPacket(0x003D, PacketMessagePlayer.class);

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
        registerPacket(0x003B, new PacketTransferPlayer());
        registerPacket(0x003C, new PacketDisconnectPlayer());
        registerPacket(0x003D, new PacketMessagePlayer());


        // 70-7F: External Sync Packets
      //registerPacket(0x0070, PacketInExRunEvent.class);
      //registerPacket(0x0071, PacketInExReset.class);
      //registerPacket(0x0073, PacketInExReload.class);
        registerPacket(0x0074, PacketExSyncPlayer.class);
        registerPacket(0x0075, PacketExTransferPlayer.class);
        registerPacket(0x0076, PacketExDisconnectPlayer.class);
        registerPacket(0x0077, PacketExMessagePlayer.class);

        registerPacket(0x0070, new PacketInExRunEvent(plugin));
        registerPacket(0x0071, new PacketInExReset());
        registerPacket(0x0073, new PacketInExEditServer(plugin));
        registerPacket(0x0074, new PacketExSyncPlayer(plugin));
        registerPacket(0x0075, new PacketExTransferPlayer(plugin));
        registerPacket(0x0076, new PacketExDisconnectPlayer(plugin));
        registerPacket(0x0077, new PacketExMessagePlayer(plugin));
    }

    public static SubProtocol get() {
        if (instance == null)
            instance = new SubProtocol();

        return instance;
    }

    private Logger getLogger(int channel) {
        return net.ME1312.SubServers.Velocity.Library.Compatibility.Logger.get("SubData" + ((channel != 0)?File.separator+"+"+channel:""));
    }

    @Override
    protected SubDataClient sub(Consumer<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        ExProxy plugin = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Try.all.get(() -> Util.reflect(ExProxy.class.getDeclaredField("subdata"), plugin), null);

        int channel = 1;
        while (map.containsKey(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port, login);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkProxy(plugin, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Consumer<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        ExProxy plugin = SubAPI.getInstance().getInternals();
        SubDataClient subdata = super.open(scheduler, logger, address, port);
        HashMap<Integer, SubDataClient> map = Try.all.get(() -> Util.reflect(ExProxy.class.getDeclaredField("subdata"), plugin));

        subdata.sendPacket(new PacketLinkProxy(plugin, 0));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.sendPacket(new PacketDownloadPlatformInfo(platform -> {
            if (plugin.lastReload != platform.getMap("subservers").getLong("last-reload")) {
                net.ME1312.SubServers.Velocity.Library.Compatibility.Logger.get("SubServers").info("Resetting Server Data");
                for (RegisteredServer server : ExProxy.getInstance().getAllServers()) ExProxy.getInstance().unregisterServer(server.getServerInfo());
                plugin.servers.clear();
                plugin.lastReload = platform.getMap("subservers").getLong("last-reload");
            } /*
            try {
                ProxyConfig config = ExProxy.getInstance().getConfiguration(); // TODO maybe?
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Forced-Hosts", true)) Util.reflect(ListenerInfo.class.getDeclaredField("forcedHosts"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getMap("forced-hosts").get());
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Motd", false)) Util.reflect(ListenerInfo.class.getDeclaredField("motd"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getString("motd"));
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Player-Limit", false)) Util.reflect(ListenerInfo.class.getDeclaredField("maxPlayers"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getInt("player-limit"));
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Server-Priorities", true)) Util.reflect(ListenerInfo.class.getDeclaredField("serverPriority"), listeners.get(i), platform.getMap("bungee").getMapList("listeners").get(i).getStringList("priorities"));
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Disabled-Commands", false)) Util.reflect(Configuration.class.getDeclaredField("disabledCommands"), plugin.getConfig(), platform.getMap("bungee").getStringList("disabled-cmds"));
                if (plugin.config.get().getMap("Sync", new ObjectMap<>()).getBoolean("Player-Limit", false)) Util.reflect(Configuration.class.getDeclaredField("playerLimit"), plugin.getConfig(), platform.getMap("bungee").getInt("player-limit"));
            } catch (Exception e) {
                net.ME1312.SubServers.Velocity.Library.Compatibility.Logger.get("SubServers").info("Problem converting synced BungeeCord configuration options");
                e.printStackTrace();
            } */

            ArrayList<CachedPlayer> localPlayers = new ArrayList<CachedPlayer>();
            for (UUID id : new ArrayList<UUID>(plugin.rPlayers.keySet())) {
                if (ExProxy.getInstance().getPlayer(id).isPresent()) {
                    localPlayers.add(plugin.rPlayers.get(id));
                } else {
                    plugin.rPlayerLinkS.remove(id);
                    plugin.rPlayerLinkP.remove(id);
                    plugin.rPlayers.remove(id);
                }
            }
            subdata.sendPacket(new PacketExSyncPlayer(null, localPlayers.toArray(new CachedPlayer[0])));

            plugin.api.getServers(servers -> {
                ArrayList<ServerData> localServers = new ArrayList<ServerData>(plugin.servers.values());
                for (Server server : servers.values()) {
                    ExProxy.getInstance().getServer(server.getName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(localServers::remove);
                    plugin.merge(server);
                }
                for (ServerData server : localServers) {
                    plugin.remove(new SubRemoveServerEvent(null, null, server.getName()));
                }

                plugin.api.getRemotePlayers(players -> {
                    synchronized (plugin.rPlayers) {
                        plugin.rPlayerLinkS.clear();
                        plugin.rPlayerLinkP.clear();
                        plugin.rPlayers.clear();
                        for (RemotePlayer player : players.values()) {
                            plugin.rPlayerLinkP.put(player.getUniqueId(), player.getProxyName().toLowerCase());
                            plugin.rPlayers.put(player.getUniqueId(), (CachedPlayer) player);

                            ExProxy.getInstance().getServer(player.getServerName()).map(RegisteredServer::getServerInfo).map(plugin::getData).ifPresent(server ->
                                    plugin.rPlayerLinkS.put(player.getUniqueId(), server)
                            );
                        }
                    }
                });
            });

        }));
        subdata.on.ready(client -> ExProxy.getInstance().getEventManager().fire(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.value(), client.key());
            ExProxy.getInstance().getEventManager().fire(event);

            if (plugin.running) {
                Logger log = net.ME1312.SubServers.Velocity.Library.Compatibility.Logger.get("SubData");
                Try.all.run(() -> Util.reflect(ExProxy.class.getDeclaredMethod("connect", Logger.class, Pair.class), plugin, log, client));
            } else map.put(0, null);
        });

        return subdata;
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
