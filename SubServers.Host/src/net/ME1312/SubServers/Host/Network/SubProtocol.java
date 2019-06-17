package net.ME1312.SubServers.Host.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Host.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Host.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Network.Packet.*;
import net.ME1312.SubServers.Host.SubAPI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private SubProtocol() {}

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            instance = new SubProtocol();

            ExHost host = SubAPI.getInstance().getInternals();

            instance.setName("SubServers 2");
            instance.addVersion(new Version("2.14a+"));


         // 00-09: Object Link Packets
            instance.registerPacket(0x0001, PacketLinkExHost.class);

            instance.registerPacket(0x0001, new PacketLinkExHost(host));


         // 10-29: Download Packets
            instance.registerPacket(0x0010, PacketDownloadLang.class);
            instance.registerPacket(0x0011, PacketDownloadPlatformInfo.class);
            instance.registerPacket(0x0012, PacketDownloadProxyInfo.class);
            instance.registerPacket(0x0013, PacketDownloadHostInfo.class);
            instance.registerPacket(0x0014, PacketDownloadGroupInfo.class);
            instance.registerPacket(0x0015, PacketDownloadServerInfo.class);
            instance.registerPacket(0x0016, PacketDownloadPlayerList.class);
            instance.registerPacket(0x0017, PacketCheckPermission.class);

            instance.registerPacket(0x0010, new PacketDownloadLang(host));
            instance.registerPacket(0x0011, new PacketDownloadPlatformInfo());
            instance.registerPacket(0x0012, new PacketDownloadProxyInfo());
            instance.registerPacket(0x0013, new PacketDownloadHostInfo());
            instance.registerPacket(0x0014, new PacketDownloadGroupInfo());
            instance.registerPacket(0x0015, new PacketDownloadServerInfo());
            instance.registerPacket(0x0016, new PacketDownloadPlayerList());
            instance.registerPacket(0x0017, new PacketCheckPermission());


         // 30-49: Control Packets
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


         // 50-69: External Host Packets
            instance.registerPacket(0x0050, PacketExConfigureHost.class);
            instance.registerPacket(0x0051, PacketExDownloadTemplates.class);
            instance.registerPacket(0x0052, PacketOutExRequestQueue.class);
            instance.registerPacket(0x0053, PacketExCreateServer.class);
            instance.registerPacket(0x0054, PacketExAddServer.class);
            instance.registerPacket(0x0055, PacketExEditServer.class);
            instance.registerPacket(0x0056, PacketOutExLogMessage.class);
            instance.registerPacket(0x0057, PacketExDeleteServer.class);
            instance.registerPacket(0x0058, PacketExRemoveServer.class);

            instance.registerPacket(0x0050, new PacketExConfigureHost(host));
            instance.registerPacket(0x0051, new PacketExDownloadTemplates(host));
          //instance.registerPacket(0x0052, new PacketOutExRequestQueue(host));
            instance.registerPacket(0x0053, new PacketExCreateServer(host));
            instance.registerPacket(0x0054, new PacketExAddServer(host));
            instance.registerPacket(0x0055, new PacketExEditServer(host));
          //instance.registerPacket(0x0056, new PacketOutExLogMessage());
            instance.registerPacket(0x0057, new PacketExDeleteServer(host));
            instance.registerPacket(0x0058, new PacketExRemoveServer(host));


         // 70-79: External Misc Packets
          //instance.registerPacket(0x0070, PacketInExRunEvent.class);
          //instance.registerPacket(0x0071, PacketInExReset.class);
          //instance.registerPacket(0x0072, PacketInExReload.class);

            instance.registerPacket(0x0070, new PacketInExRunEvent());
            instance.registerPacket(0x0071, new PacketInExReset(host));
            instance.registerPacket(0x0072, new PacketInExReload(host));
        }

        return instance;
    }

    private Logger getLogger(int channel) {
        return new net.ME1312.Galaxi.Library.Log.Logger("SubData" + ((channel != 0)?File.separator+"Sub-"+channel:"")).toPrimitive();
    }

    @Override
    protected SubDataClient sub(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        ExHost host = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(ExHost.class.getDeclaredField("subdata"), host), null);

        int channel = 1;
        while (map.keySet().contains(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkExHost(host, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        ExHost host = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(ExHost.class.getDeclaredField("subdata"), host), null);

        SubDataClient subdata = super.open(scheduler, logger, address, port);
        subdata.sendPacket(new PacketLinkExHost(host, 0));
        subdata.sendPacket(new PacketExConfigureHost(host));
        subdata.sendPacket(new PacketExDownloadTemplates(host));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.sendPacket(new PacketOutExRequestQueue());
        subdata.on.ready(client -> host.engine.getPluginManager().executeEvent(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            host.engine.getPluginManager().executeEvent(event);
            map.put(0, null);

            Logger log = Util.getDespiteException(() -> Util.reflect(SubDataClient.class.getDeclaredField("log"), client.get()), null);
            int reconnect = host.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 30);
            if (Util.getDespiteException(() -> Util.reflect(ExHost.class.getDeclaredField("reconnect"), host), false) && reconnect > 0
                    && client.name() != DisconnectReason.PROTOCOL_MISMATCH && client.name() != DisconnectReason.ENCRYPTION_MISMATCH) {
                log.info("Attempting reconnect in " + reconnect + " seconds");
                Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::SubData_Reconnect_Handler");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Util.reflect(ExHost.class.getDeclaredMethod("connect"), host);
                            timer.cancel();
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof IOException) {
                                log.info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
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
