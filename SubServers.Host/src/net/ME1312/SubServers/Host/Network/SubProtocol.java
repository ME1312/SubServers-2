package net.ME1312.SubServers.Host.Network;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Common.Network.Packet.*;
import net.ME1312.SubServers.Host.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Host.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Network.Packet.*;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * SubServers Protocol Class
 */
public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;

    @SuppressWarnings("deprecation")
    protected SubProtocol() {
        ExHost host = SubAPI.getInstance().getInternals();

        setName("SubServers 2");
        addVersion(new Version("2.18a+"));


     // 00-0F: Object Link Packets
        registerPacket(0x0001, PacketLinkExHost.class);

        registerPacket(0x0001, new PacketLinkExHost(host));


     // 10-2F: Download Packets
        registerPacket(0x0010, PacketDownloadLang.class);
        registerPacket(0x0011, PacketDownloadPlatformInfo.class);
        registerPacket(0x0012, PacketDownloadProxyInfo.class);
        registerPacket(0x0013, PacketDownloadHostInfo.class);
        registerPacket(0x0014, PacketDownloadGroupInfo.class);
        registerPacket(0x0015, PacketDownloadServerInfo.class);
        registerPacket(0x0016, PacketDownloadPlayerInfo.class);
        registerPacket(0x0017, PacketCheckPermission.class);
        registerPacket(0x0017, PacketCheckPermissionResponse.class);

        registerPacket(0x0010, new PacketDownloadLang(host));
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


     // 50-6F: External Host Packets
        registerPacket(0x0050, PacketExConfigureHost.class);
        registerPacket(0x0051, PacketExUploadTemplates.class);
        registerPacket(0x0052, PacketExDownloadTemplates.class);
        registerPacket(0x0053, PacketOutExRequestQueue.class);
        registerPacket(0x0054, PacketExCreateServer.class);
        registerPacket(0x0055, PacketExAddServer.class);
        registerPacket(0x0056, PacketExControlServer.class);
        registerPacket(0x0057, PacketOutExLogMessage.class);
        registerPacket(0x0058, PacketExRemoveServer.class);
        registerPacket(0x0059, PacketExDeleteServer.class);

        registerPacket(0x0050, new PacketExConfigureHost(host));
        registerPacket(0x0051, new PacketExUploadTemplates(host));
        registerPacket(0x0052, new PacketExDownloadTemplates(host));
      //registerPacket(0x0053, new PacketOutExRequestQueue(host));
        registerPacket(0x0054, new PacketExCreateServer(host));
        registerPacket(0x0055, new PacketExAddServer(host));
        registerPacket(0x0056, new PacketExControlServer(host));
      //registerPacket(0x0057, new PacketOutExLogMessage());
        registerPacket(0x0058, new PacketExRemoveServer(host));
        registerPacket(0x0059, new PacketExDeleteServer(host));


     // 70-7F: External Sync Packets
      //registerPacket(0x0070, PacketInExRunEvent.class);
      //registerPacket(0x0071, PacketInExReset.class);
      //registerPacket(0x0072, PacketInExReload.class);

        registerPacket(0x0070, new PacketInExRunEvent());
        registerPacket(0x0071, new PacketInExReset(host));
        registerPacket(0x0072, new PacketInExReload(host));
    }

    public static SubProtocol get() {
        if (instance == null)
            instance = new SubProtocol();

        return instance;
    }

    private Logger getLogger(int channel) {
        return new net.ME1312.Galaxi.Log.Logger("SubData" + ((channel != 0)?File.separator+"+"+channel:"")).toPrimitive();
    }

    @Override
    protected SubDataClient sub(Consumer<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        ExHost host = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Try.all.get(() -> Util.reflect(ExHost.class.getDeclaredField("subdata"), host));

        int channel = 1;
        while (map.containsKey(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port, login);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkExHost(host, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Consumer<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        ExHost host = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Try.all.get(() -> Util.reflect(ExHost.class.getDeclaredField("subdata"), host));

        SubDataClient subdata = super.open(scheduler, logger, address, port);
        subdata.sendPacket(new PacketLinkExHost(host, 0));
        subdata.sendPacket(new PacketExConfigureHost(host));
        subdata.sendPacket(new PacketExUploadTemplates(host));
        if (host.config.get().getMap("Settings").getBoolean("Download-Templates", true))
            subdata.sendPacket(new PacketExDownloadTemplates(host));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.sendPacket(new PacketOutExRequestQueue());
        subdata.on.ready(client -> host.engine.getPluginManager().executeEvent(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.value(), client.key());
            host.engine.getPluginManager().executeEvent(event);

            if (Try.all.get(() -> Util.reflect(ExHost.class.getDeclaredField("running"), host), true)) {
                Logger log = Try.all.get(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.value()));
                Try.all.run(() -> Util.reflect(ExHost.class.getDeclaredMethod("connect", Logger.class, Pair.class), host, log, client));
            } else map.put(0, null);
        });

        return subdata;
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
