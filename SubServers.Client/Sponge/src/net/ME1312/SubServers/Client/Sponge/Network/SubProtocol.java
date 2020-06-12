package net.ME1312.SubServers.Client.Sponge.Network;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Sponge.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Client.Sponge.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.*;
import net.ME1312.SubServers.Client.Sponge.SubAPI;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

public class SubProtocol extends SubDataProtocol {
    private static SubProtocol instance;
    private SubProtocol() {}

    @SuppressWarnings("deprecation")
    public static SubProtocol get() {
        if (instance == null) {
            instance = new SubProtocol();

            SubPlugin plugin = SubAPI.getInstance().getInternals();

            instance.setName("SubServers 2");
            instance.addVersion(new Version("2.16a+"));


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
            instance.registerPacket(0x0016, PacketDownloadPlayerInfo.class);
            instance.registerPacket(0x0017, PacketCheckPermission.class);
            instance.registerPacket(0x0018, PacketCheckPermissionResponse.class);

            instance.registerPacket(0x0010, new PacketDownloadLang(plugin));
            instance.registerPacket(0x0011, new PacketDownloadPlatformInfo());
            instance.registerPacket(0x0012, new PacketDownloadProxyInfo());
            instance.registerPacket(0x0013, new PacketDownloadHostInfo());
            instance.registerPacket(0x0014, new PacketDownloadGroupInfo());
            instance.registerPacket(0x0015, new PacketDownloadServerInfo());
            instance.registerPacket(0x0016, new PacketDownloadPlayerInfo());
            instance.registerPacket(0x0017, new PacketCheckPermission());
            instance.registerPacket(0x0018, new PacketCheckPermissionResponse());


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

            instance.registerPacket(0x0070, new PacketInExRunEvent(plugin));
            instance.registerPacket(0x0071, new PacketInExReset());
            instance.registerPacket(0x0072, new PacketInExReload(plugin));
        }

        return instance;
    }

    private Logger getLogger(int channel) {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            private org.slf4j.Logger log = LoggerFactory.getLogger("SubData" + ((channel != 0)? "/Sub-"+channel:""));
            private boolean open = true;

            @Override
            public void publish(LogRecord record) {
                if (open) {
                    if (record.getLevel().intValue() == OFF.intValue()) {
                        // do nothing
                    } else if (record.getLevel().intValue() == FINE.intValue() || record.getLevel().intValue() == FINER.intValue() || record.getLevel().intValue() == FINEST.intValue()) {
                        log.debug(record.getMessage());
                    } else if (record.getLevel().intValue() == ALL.intValue() || record.getLevel().intValue() == CONFIG.intValue() || record.getLevel().intValue() == INFO.intValue()) {
                        log.info(record.getMessage());
                    } else if (record.getLevel().intValue() == WARNING.intValue()) {
                        log.warn(record.getMessage());
                    } else if (record.getLevel().intValue() == SEVERE.intValue()) {
                        log.error(record.getMessage());
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
    protected SubDataClient sub(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("subdata"), plugin), null);

        int channel = 1;
        while (map.keySet().contains(channel)) channel++;
        final int fc = channel;

        SubDataClient subdata = super.open(scheduler, getLogger(fc), address, port, login);
        map.put(fc, subdata);
        subdata.sendPacket(new PacketLinkServer(plugin, fc));
        subdata.on.closed(client -> map.remove(fc));

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        HashMap<Integer, SubDataClient> map = Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("subdata"), plugin), null);

        SubDataClient subdata = super.open(scheduler, logger, address, port);
        subdata.sendPacket(new PacketLinkServer(plugin, 0));
        subdata.sendPacket(new PacketDownloadLang());
        subdata.on.ready(client -> Sponge.getEventManager().post(new SubNetworkConnectEvent((SubDataClient) client)));
        subdata.on.closed(client -> {
            SubNetworkDisconnectEvent event = new SubNetworkDisconnectEvent(client.get(), client.name());
            Sponge.getEventManager().post(event);

            if (Util.getDespiteException(() -> Util.reflect(SubPlugin.class.getDeclaredField("running"), plugin), true)) {
                Util.isException(() -> Util.reflect(SubPlugin.class.getDeclaredMethod("connect", NamedContainer.class), plugin, client));
            } else map.put(0, null);
        });

        return subdata;
    }

    @SuppressWarnings("deprecation")
    @Override
    public SubDataClient open(Logger logger, InetAddress address, int port) throws IOException {
        SubPlugin plugin = SubAPI.getInstance().getInternals();
        return open(event -> Sponge.getScheduler().createTaskBuilder().async().execute(event).submit(plugin), logger, address, port);
    }

    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(getLogger(0), address, port);
    }
}
