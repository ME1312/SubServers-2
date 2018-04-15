package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

/**
 * Server Log Listener Packet
 */
public class PacketListenLog implements PacketIn, PacketOut {
    private static HashMap<String, NamedContainer<SubLogger, SubLogFilter>> filters = new HashMap<String, NamedContainer<SubLogger, SubLogFilter>>();
    private SubPlugin plugin;
    private SubLogger.LogMessage[] lines;
    private String id;

    /**
     * New PacketListenServerLog (In)
     *
     * @param plugin SubPlugin
     */
    public PacketListenLog(SubPlugin plugin) {
        if (Util.isNull(plugin)) throw new NullPointerException();
        this.plugin = plugin;
    }

    /**
     * New PacketListenServerLog (Out)
     *
     * @param line Message
     * @param id Receiver ID
     */
    public PacketListenLog(String id, SubLogger.LogMessage... line) {
        if (Util.isNull(id, line)) throw new NullPointerException();
        this.lines = line;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection add = new YAMLSection();
        add.set("id", id);
        LinkedList<String> lines = new LinkedList<String>();
        for (SubLogger.LogMessage line : this.lines) lines.add(new SimpleDateFormat("hh:mm:ss").format(line.getDate()) + " [" + line.getLevel().getLocalizedName() + "] " + line.getMessage());
        add.set("lines", lines);
        return add;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        if (data != null && data.contains("id")) {
            if (data.contains("server")) {
                if (data.getRawString("server").length() == 0) {
                    unregister(data.getRawString("id"));
                } else {
                    Map<String, SubServer> servers = plugin.api.getSubServers();
                    if (servers.keySet().contains(data.getRawString("server").toLowerCase())) {
                        register(client, data.getRawString("id"), servers.get(data.getRawString("server").toLowerCase()).getLogger());
                    }
                }
            } else if (data.contains("creator")) {
                if (data.getRawString("creator").length() == 0) {
                    unregister(data.getRawString("id"));
                } else {
                    Map<String, Host> hosts = plugin.api.getHosts();
                    if (hosts.keySet().contains(data.getRawString("creator").toLowerCase())) {
                        register(client, data.getRawString("id"), hosts.get(data.getRawString("creator").toLowerCase()).getCreator().getLogger(data.getRawString("name")));
                    }
                }
            }
        }
    }

    private void register(Client client, String id, SubLogger logger) {
        client.sendPacket(new PacketListenLog(id, logger.getMessageHistory().toArray(new SubLogger.LogMessage[logger.getMessageHistory().size()])));
        SubLogFilter filter = new SubLogFilter() {
            @Override
            public void start() {
                if (client.getConnection().isClosed()) {
                    unregister(id);
                }
            }

            @Override
            public boolean log(Level level, String message) {
                if (client.getConnection().isClosed()) {
                    unregister(id);
                } else {
                    client.sendPacket(new PacketListenLog(id, new SubLogger.LogMessage(level, message)));
                }
                return true;
            }

            @Override
            public void stop() {
                if (client.getConnection().isClosed()) {
                    unregister(id);
                }
            }
        };
        filters.put(id, new NamedContainer<SubLogger, SubLogFilter>(logger, filter));
        logger.registerFilter(filter);
    }

    private void unregister(String id) {
        if (filters.keySet().contains(id)) {
            filters.get(id).name().unregisterFilter(filters.get(id).get());
            filters.remove(id);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
