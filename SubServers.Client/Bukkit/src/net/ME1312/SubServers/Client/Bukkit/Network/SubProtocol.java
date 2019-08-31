package net.ME1312.SubServers.Client.Bukkit.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Bukkit.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Client.Bukkit.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
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
            instance.registerPacket(0x0002, PacketLinkServer.class);
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
          //instance.registerPacket(0x0072, PacketInExReload.class);
            instance.registerPacket(0x0074, PacketExCheckPermission.class);

            instance.registerPacket(0x0070, new PacketInExRunEvent(plugin));
            instance.registerPacket(0x0071, new PacketInExReset());
            instance.registerPacket(0x0072, new PacketInExReload(plugin));
            instance.registerPacket(0x0074, new PacketExCheckPermission());
        }

        return instance;
    }

    @SuppressWarnings("deprecation")
    private Logger getLogger(int channel) {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            private boolean open = true;
            private String prefix = "SubData" + ((channel != 0)? "/Sub-"+channel:"");

            @Override
            public void publish(LogRecord record) {
                if (open) {
                    if (plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getLogger().log(record.getLevel(), prefix + " > " + record.getMessage(), record.getParameters()));
                    } else {
                        Bukkit.getLogger().log(record.getLevel(), prefix + " > " + record.getMessage(), record.getParameters());
                    }
                }
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {
                open = false;
            }
        });

        return log;
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
        subdata.sendPacket(new PacketLinkServer(plugin, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        SubDataClient subdata = super.open(scheduler, logger, address, port);
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("subdata"), plugin), null);

        subdata.sendPacket(new PacketLinkServer(plugin, 0));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.on.ready(client -> Bukkit.getPluginManager().callEvent(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            if (plugin.isEnabled()) Bukkit.getPluginManager().callEvent(event);
            map.put(0, null);

            Bukkit.getLogger().info("SubData > Attempting reconnect in " + plugin.config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 30) + " seconds");
            Util.isException(() -> Util.reflect(SubPlugin.class.getDeclaredMethod("connect", NamedContainer.class), plugin, client));
        });

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        return open(event -> {
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, event);
            else event.run();
        }, logger, address, port);
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
