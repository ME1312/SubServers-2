package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataProtocol;
import net.ME1312.SubData.Server.SubDataServer;
import net.ME1312.SubServers.Bungee.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkLoginEvent;
import net.ME1312.SubServers.Bungee.Network.Packet.*;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private static Logger log;
    private SubProtocol() {}

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            instance = new SubProtocol();
            log = net.ME1312.SubServers.Bungee.Library.Compatibility.Logger.get("SubData");
            SubPlugin plugin = SubAPI.getInstance().getInternals();
            plugin.getPluginManager().registerListener(null, new PacketOutExRunEvent(plugin));

            instance.setName("SubServers 2");
            instance.setVersion(new Version("2.14a+"));


         // 00-0F: Object Link Packets
            instance.registerPacket(0x0000, PacketLinkProxy.class);
            instance.registerPacket(0x0001, PacketLinkExHost.class);
            instance.registerPacket(0x0002, PacketLinkServer.class);

            instance.registerPacket(0x0000, new PacketLinkProxy(plugin));
            instance.registerPacket(0x0001, new PacketLinkExHost(plugin));
            instance.registerPacket(0x0002, new PacketLinkServer(plugin));


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
            instance.registerPacket(0x0011, new PacketDownloadPlatformInfo(plugin));
            instance.registerPacket(0x0012, new PacketDownloadProxyInfo(plugin));
            instance.registerPacket(0x0013, new PacketDownloadHostInfo(plugin));
            instance.registerPacket(0x0014, new PacketDownloadGroupInfo(plugin));
            instance.registerPacket(0x0015, new PacketDownloadServerInfo(plugin));
            instance.registerPacket(0x0016, new PacketDownloadPlayerList(plugin));
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

            instance.registerPacket(0x0030, new PacketCreateServer(plugin));
            instance.registerPacket(0x0031, new PacketAddServer(plugin));
            instance.registerPacket(0x0032, new PacketStartServer(plugin));
            instance.registerPacket(0x0033, new PacketUpdateServer(plugin));
            instance.registerPacket(0x0034, new PacketEditServer(plugin));
            instance.registerPacket(0x0035, new PacketRestartServer(plugin));
            instance.registerPacket(0x0036, new PacketCommandServer(plugin));
            instance.registerPacket(0x0037, new PacketStopServer(plugin));
            instance.registerPacket(0x0038, new PacketRemoveServer(plugin));
            instance.registerPacket(0x0039, new PacketDeleteServer(plugin));


         // 50-6F: External Host Packets
            instance.registerPacket(0x0050, PacketExConfigureHost.class);
            instance.registerPacket(0x0051, PacketExDownloadTemplates.class);
          //instance.registerPacket(0x0052, PacketInExRequestQueue.class);
            instance.registerPacket(0x0053, PacketExCreateServer.class);
            instance.registerPacket(0x0054, PacketExAddServer.class);
            instance.registerPacket(0x0055, PacketExEditServer.class);
          //instance.registerPacket(0x0056, PacketInExLogMessage.class);
            instance.registerPacket(0x0057, PacketExDeleteServer.class);
            instance.registerPacket(0x0058, PacketExRemoveServer.class);

            instance.registerPacket(0x0050, new PacketExConfigureHost(plugin));
            instance.registerPacket(0x0051, new PacketExDownloadTemplates(plugin));
            instance.registerPacket(0x0052, new PacketInExRequestQueue(plugin));
            instance.registerPacket(0x0053, new PacketExCreateServer(null));
            instance.registerPacket(0x0054, new PacketExAddServer());
            instance.registerPacket(0x0055, new PacketExEditServer(plugin));
            instance.registerPacket(0x0056, new PacketInExLogMessage());
            instance.registerPacket(0x0057, new PacketExDeleteServer());
            instance.registerPacket(0x0058, new PacketExRemoveServer());


         // 70-7F: External Misc Packets
            instance.registerPacket(0x0070, PacketOutExRunEvent.class);
            instance.registerPacket(0x0071, PacketOutExReset.class);
            instance.registerPacket(0x0072, PacketOutExReload.class);
            instance.registerPacket(0x0073, PacketOutExUpdateWhitelist.class);
            instance.registerPacket(0x0074, PacketExCheckPermission.class);

          //instance.registerPacket(0x0070, new PacketOutRunEvent());
          //instance.registerPacket(0x0071, new PacketOutReset());
          //instance.registerPacket(0x0072, new PacketOutReload());
          //instance.registerPacket(0x0073, new PacketOutExUpdateWhitelist());
            instance.registerPacket(0x0074, new PacketExCheckPermission());
        }

        return instance;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataServer open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, String cipher) throws IOException {
        SubDataServer subdata = super.open(scheduler, logger, address, port, cipher);
        SubPlugin plugin = SubAPI.getInstance().getInternals();

        subdata.on.closed(server -> plugin.subdata = null);
        subdata.on.connect(client -> {
            if (!plugin.getPluginManager().callEvent(new SubNetworkConnectEvent(client.getServer(), client)).isCancelled()) {
                client.on.ready(c -> plugin.getPluginManager().callEvent(new SubNetworkLoginEvent(c.getServer(), c)));
                client.on.closed(c -> plugin.getPluginManager().callEvent(new SubNetworkDisconnectEvent(c.get().getServer(), c.get(), c.name())));
                return true;
            } else return false;
        });

        return subdata;
    }

    public SubDataServer open(InetAddress address, int port, String cipher) throws IOException {
        return open(log, address, port, cipher);
    }
}
