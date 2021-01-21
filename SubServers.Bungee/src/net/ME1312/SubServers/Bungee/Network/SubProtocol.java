package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Library.DataSize;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataProtocol;
import net.ME1312.SubData.Server.SubDataServer;
import net.ME1312.SubServers.Bungee.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkLoginEvent;
import net.ME1312.SubServers.Bungee.Network.Packet.*;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * SubServers Protocol Class
 */
public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private static Logger log;

    @SuppressWarnings("deprecation")
    protected SubProtocol() {
        SubProxy plugin = SubAPI.getInstance().getInternals();

        setName("SubServers 2");
        setVersion(new Version("2.16a+"));
        setBlockSize(DataSize.MB);


     // 00-0F: Object Link Packets
        registerPacket(0x0000, PacketLinkProxy.class);
        registerPacket(0x0001, PacketLinkExHost.class);
        registerPacket(0x0002, PacketLinkServer.class);

        registerPacket(0x0000, new PacketLinkProxy(plugin));
        registerPacket(0x0001, new PacketLinkExHost(plugin));
        registerPacket(0x0002, new PacketLinkServer(plugin));


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
        registerPacket(0x0011, new PacketDownloadPlatformInfo(plugin));
        registerPacket(0x0012, new PacketDownloadProxyInfo(plugin));
        registerPacket(0x0013, new PacketDownloadHostInfo(plugin));
        registerPacket(0x0014, new PacketDownloadGroupInfo(plugin));
        registerPacket(0x0015, new PacketDownloadServerInfo(plugin));
        registerPacket(0x0016, new PacketDownloadPlayerInfo(plugin));
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
      //registerPacket(0x003A, PacketRestoreServer.class); // TODO
      //registerPacket(0x003B, PacketTeleportPlayer.class);
        registerPacket(0x003C, PacketDisconnectPlayer.class);

        registerPacket(0x0030, new PacketCreateServer(plugin));
        registerPacket(0x0031, new PacketAddServer(plugin));
        registerPacket(0x0032, new PacketStartServer(plugin));
        registerPacket(0x0033, new PacketUpdateServer(plugin));
        registerPacket(0x0034, new PacketEditServer(plugin));
        registerPacket(0x0035, new PacketRestartServer(plugin));
        registerPacket(0x0036, new PacketCommandServer(plugin));
        registerPacket(0x0037, new PacketStopServer(plugin));
        registerPacket(0x0038, new PacketRemoveServer(plugin));
        registerPacket(0x0039, new PacketDeleteServer(plugin));
      //registerPacket(0x003A, new PacketRestoreServer(plugin)); // TODO
      //registerPacket(0x003B, new PacketTeleportPlayer(plugin));
        registerPacket(0x003C, new PacketDisconnectPlayer(plugin));


     // 50-6F: External Host Packets
        registerPacket(0x0050, PacketExConfigureHost.class);
        registerPacket(0x0051, PacketExUploadTemplates.class);
        registerPacket(0x0052, PacketExDownloadTemplates.class);
      //registerPacket(0x0053, PacketInExRequestQueue.class);
        registerPacket(0x0054, PacketExCreateServer.class);
        registerPacket(0x0055, PacketExAddServer.class);
        registerPacket(0x0056, PacketExEditServer.class);
      //registerPacket(0x0057, PacketInExLogMessage.class);
        registerPacket(0x0058, PacketExRemoveServer.class);
        registerPacket(0x0059, PacketExDeleteServer.class);
      //registerPacket(0x005A, PacketExRestoreServer.class);

        registerPacket(0x0050, new PacketExConfigureHost(plugin));
        registerPacket(0x0051, new PacketExUploadTemplates(plugin));
        registerPacket(0x0052, new PacketExDownloadTemplates(plugin));
        registerPacket(0x0053, new PacketInExRequestQueue(plugin));
        registerPacket(0x0054, new PacketExCreateServer(null));
        registerPacket(0x0055, new PacketExAddServer());
        registerPacket(0x0056, new PacketExEditServer(plugin));
        registerPacket(0x0057, new PacketInExLogMessage());
        registerPacket(0x0058, new PacketExRemoveServer());
        registerPacket(0x0059, new PacketExDeleteServer());
      //registerPacket(0x005A, new PacketExRestoreServer());


     // 70-7F: External Misc Packets
        registerPacket(0x0070, PacketOutExRunEvent.class);
        registerPacket(0x0071, PacketOutExReset.class);
        registerPacket(0x0072, PacketOutExReload.class);
        registerPacket(0x0073, PacketOutExUpdateWhitelist.class);
        registerPacket(0x0074, PacketExSyncPlayer.class);
        registerPacket(0x0076, PacketExDisconnectPlayer.class);

      //registerPacket(0x0070, new PacketOutRunEvent());
      //registerPacket(0x0071, new PacketOutReset());
      //registerPacket(0x0072, new PacketOutReload());
      //registerPacket(0x0073, new PacketOutExUpdateWhitelist());
        registerPacket(0x0074, new PacketExSyncPlayer(plugin));
        registerPacket(0x0076, new PacketExDisconnectPlayer());
    }

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            instance = new SubProtocol();
            log = net.ME1312.SubServers.Bungee.Library.Compatibility.Logger.get("SubData");
            SubProxy plugin = SubAPI.getInstance().getInternals();
            plugin.getPluginManager().registerListener(plugin.plugin, new PacketOutExRunEvent(plugin));
        }

        return instance;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataServer open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, String cipher) throws IOException {
        SubDataServer subdata = super.open(scheduler, logger, address, port, cipher);
        SubProxy plugin = SubAPI.getInstance().getInternals();

        subdata.on.closed(server -> plugin.subdata = null);
        subdata.on.connect(client -> {
            if (!plugin.getPluginManager().callEvent(new SubNetworkConnectEvent(client.getServer(), client)).isCancelled()) {
                client.on.ready(c -> {
                    ((SubDataClient) c).setBlockSize((int) DataSize.KBB);
                    plugin.getPluginManager().callEvent(new SubNetworkLoginEvent(c.getServer(), c));
                });
                client.on.closed(c -> plugin.getPluginManager().callEvent(new SubNetworkDisconnectEvent(c.value().getServer(), c.value(), c.key())));
                return true;
            } else return false;
        });

        return subdata;
    }

    public SubDataServer open(InetAddress address, int port, String cipher) throws IOException {
        return open(log, address, port, cipher);
    }
}
