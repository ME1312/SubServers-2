package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubLogFilter;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
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
    public JSONObject generate() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        JSONArray lines = new JSONArray();
        for (SubLogger.LogMessage line : this.lines) lines.put(new SimpleDateFormat("hh:mm:ss").format(line.getDate()) + " [" + line.getLevel().getLocalizedName() + "] " + line.getMessage());
        json.put("lines", lines);
        return json;
    }

    @Override
    public void execute(Client client, JSONObject data) {
        if (data != null && data.keySet().contains("id")) {
            if (data.keySet().contains("server")) {
                if (data.getString("server").length() == 0) {
                    unregister(data.getString("id"));
                } else {
                    Map<String, SubServer> servers = plugin.api.getSubServers();
                    if (servers.keySet().contains(data.getString("server").toLowerCase())) {
                        register(client, data.getString("id"), servers.get(data.getString("server").toLowerCase()).getLogger());
                    }
                }
            } else if (data.keySet().contains("creator")) {
                if (data.getString("creator").length() == 0) {
                    unregister(data.getString("id"));
                } else {
                    Map<String, Host> hosts = plugin.api.getHosts();
                    if (hosts.keySet().contains(data.getString("creator").toLowerCase())) {
                        register(client, data.getString("id"), hosts.get(data.getString("creator").toLowerCase()).getCreator().getLogger(data.getString("name")));
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
