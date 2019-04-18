package net.ME1312.SubServers.Client.Bukkit.Network;

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
    private SubProtocol(Logger logger) {
        super(logger);
    }

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            Logger log = Logger.getAnonymousLogger();
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
            instance = new SubProtocol(log);
            SubPlugin plugin = SubAPI.getInstance().getInternals();

            instance.setName("SubServers 2");
            instance.addVersion(new Version("2.13.2a+"));


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
          //instance.registerPacket(0x0030, PacketOutReload.class);    TODO
            instance.registerPacket(0x0031, PacketCreateServer.class);
          //instance.registerPacket(0x0032, PacketAddServer.class);    TODO
            instance.registerPacket(0x0033, PacketStartServer.class);
          //instance.registerPacket(0x0034, PacketEditServer.class);   TODO
            instance.registerPacket(0x0035, PacketRestartServer.class);
            instance.registerPacket(0x0036, PacketCommandServer.class);
            instance.registerPacket(0x0037, PacketStopServer.class);
          //instance.registerPacket(0x0038, PacketRemoveServer.class); TODO
          //instance.registerPacket(0x0039, PacketDeleteServer.class); TODO

          //instance.registerPacket(0x0030  new PacketOutReload());    TODO
            instance.registerPacket(0x0031, new PacketCreateServer());
          //instance.registerPacket(0x0032, new PacketAddServer());    TODO
            instance.registerPacket(0x0033, new PacketStartServer());
          //instance.registerPacket(0x0034, new PacketEditServer());   TODO
            instance.registerPacket(0x0035, new PacketRestartServer());
            instance.registerPacket(0x0036, new PacketCommandServer());
            instance.registerPacket(0x0037, new PacketStopServer());
          //instance.registerPacket(0x0038, new PacketRemoveServer()); TODO
          //instance.registerPacket(0x0039, new PacketDeleteServer()); TODO


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
    public SubDataClient open(InetAddress address, int port) throws IOException {
        SubDataClient subdata = super.open(address, port);
        SubPlugin plugin = SubAPI.getInstance().getInternals();

        subdata.on.ready(client -> ((SubDataClient) client).sendPacket(new PacketLinkServer(plugin)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            Bukkit.getPluginManager().callEvent(event);
        });

        return subdata;
    }
}
