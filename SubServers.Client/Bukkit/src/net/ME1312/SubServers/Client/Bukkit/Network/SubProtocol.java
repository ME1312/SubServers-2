package net.ME1312.SubServers.Client.Bukkit.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Bukkit.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.*;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;
import net.ME1312.SubServers.Client.Bukkit.SubPlugin;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private static Logger log;
    private SubProtocol() {}

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            log = Logger.getAnonymousLogger();
            log.setUseParentHandlers(false);
            log.addHandler(new Handler() {
                private boolean open = true;

                @Override
                public void publish(LogRecord record) {
                    if (open)
                        Bukkit.getLogger().log(record.getLevel(), "SubData > " + record.getMessage(), record.getParameters());
                }

                @Override
                public void flush() {

                }

                @Override
                public void close() throws SecurityException {
                    open = false;
                }
            });
            instance = new SubProtocol();
            SubPlugin plugin = SubAPI.getInstance().getInternals();

            instance.setName("SubServers 2");
            instance.addVersion(new Version("2.14a+"));


            // 00-09: Object Link Packets
            instance.registerPacket(0x0002, PacketLinkServer.class);
            instance.registerPacket(0x0002, new PacketLinkServer(plugin));


            // 10-29: Download Packets
            instance.registerPacket(0x0010, PacketDownloadLang.class);
            instance.registerPacket(0x0011, PacketDownloadPlatformInfo.class);
            instance.registerPacket(0x0012, PacketDownloadProxyInfo.class);
            instance.registerPacket(0x0013, PacketDownloadHostInfo.class);
            instance.registerPacket(0x0014, PacketDownloadGroupInfo.class);
            instance.registerPacket(0x0015, PacketDownloadServerInfo.class);
            instance.registerPacket(0x0016, PacketDownloadPlayerList.class);

            instance.registerPacket(0x0010, new PacketDownloadLang(plugin));
            instance.registerPacket(0x0011, new PacketDownloadPlatformInfo());
            instance.registerPacket(0x0012, new PacketDownloadProxyInfo());
            instance.registerPacket(0x0013, new PacketDownloadHostInfo());
            instance.registerPacket(0x0014, new PacketDownloadGroupInfo());
            instance.registerPacket(0x0015, new PacketDownloadServerInfo());
            instance.registerPacket(0x0016, new PacketDownloadPlayerList());


            // 30-49: Control Packets
            instance.registerPacket(0x0030, PacketCreateServer.class);
            instance.registerPacket(0x0031, PacketAddServer.class);
            instance.registerPacket(0x0032, PacketStartServer.class);
            instance.registerPacket(0x0033, PacketEditServer.class);
            instance.registerPacket(0x0034, PacketRestartServer.class);
            instance.registerPacket(0x0035, PacketCommandServer.class);
            instance.registerPacket(0x0036, PacketStopServer.class);
            instance.registerPacket(0x0037, PacketRemoveServer.class);
            instance.registerPacket(0x0038, PacketDeleteServer.class);

            instance.registerPacket(0x0030, new PacketCreateServer());
            instance.registerPacket(0x0031, new PacketAddServer());
            instance.registerPacket(0x0032, new PacketStartServer());
            instance.registerPacket(0x0033, new PacketEditServer());
            instance.registerPacket(0x0034, new PacketRestartServer());
            instance.registerPacket(0x0035, new PacketCommandServer());
            instance.registerPacket(0x0036, new PacketStopServer());
            instance.registerPacket(0x0037, new PacketRemoveServer());
            instance.registerPacket(0x0038, new PacketDeleteServer());


            // 70-79: External Misc Packets
          //instance.registerPacket(0x0070, PacketInExRunEvent.class);
          //instance.registerPacket(0x0071, PacketInExReset.class);
          //instance.registerPacket(0x0072, PacketInExReload.class);

            instance.registerPacket(0x0070, new PacketInExRunEvent(plugin));
            instance.registerPacket(0x0071, new PacketInExReset());
            instance.registerPacket(0x0072, new PacketInExReload(plugin));
        }

        return instance;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        SubDataClient subdata = super.open(scheduler, logger, address, port);
        SubPlugin plugin = SubAPI.getInstance().getInternals();

        subdata.on.ready(client -> ((SubDataClient) client).sendPacket(new PacketLinkServer(plugin)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            if (plugin.isEnabled()) Bukkit.getPluginManager().callEvent(event);
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
        return open(log, address, port);
    }
}
