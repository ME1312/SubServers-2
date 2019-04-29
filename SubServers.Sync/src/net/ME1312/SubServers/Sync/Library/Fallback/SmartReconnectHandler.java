package net.ME1312.SubServers.Sync.Library.Fallback;

import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Server.ServerContainer;
import net.ME1312.SubServers.Sync.Server.SubServerContainer;
import net.ME1312.SubServers.Sync.SubAPI;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Smart Reconnect Handler Class
 */
public class SmartReconnectHandler implements ReconnectHandler {
    private static List<FallbackInspector> inspectors = new CopyOnWriteArrayList<FallbackInspector>();

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        ServerInfo forced = getForcedHost(player.getPendingConnection());
        if (forced != null) {
            return forced;
        } else {
            Map<String, ServerInfo> fallbacks = getFallbackServers(player.getPendingConnection().getListener());
            if (fallbacks.isEmpty()) {
                return null;
            } else {
                if (player instanceof UserConnection) ((UserConnection) player).setServerJoinQueue(new LinkedBlockingQueue<>(fallbacks.keySet()));
                return new LinkedList<Map.Entry<String, ServerInfo>>(fallbacks.entrySet()).getFirst().getValue();
            }
        }
    }

    /**
     * Grabs the Forced Host Server for this connection
     *
     * @see AbstractReconnectHandler#getForcedHost(PendingConnection) Essentially the same method, but more ambigous
     * @param connection Connection to check
     * @return Forced Host Server (or null if there is none)
     */
    public static ServerInfo getForcedHost(PendingConnection connection) {
        if (connection.getVirtualHost() == null) {
            return null;
        } else {
            String forced = connection.getListener().getForcedHosts().get(connection.getVirtualHost().getHostString());
            //if (forced == null && con.getListener().isForceDefault()) {   // This is the part of the method that made it ambiguous
            //    forced = con.getListener().getDefaultServer();            // Aside from that, everything else was fine
            //}                                                             // :(

            return ProxyServer.getInstance().getServerInfo(forced);
        }
    }

    /**
     * Generates a <i>smart</i> sorted map of fallback servers using a generated confidence score
     *
     * @param listener Listener to grab fallback servers from
     * @return Fallback Server Map (with legacy bungee case-sensitive keys)
     */
    @SuppressWarnings("deprecation")
    public static Map<String, ServerInfo> getFallbackServers(ListenerInfo listener) {
        TreeMap<Integer, List<ServerInfo>> score = new TreeMap<Integer, List<ServerInfo>>(Collections.reverseOrder());
        for (String name : listener.getServerPriority()) {
            ServerInfo server = SubAPI.getInstance().getInternals().servers.getOrDefault(name.toLowerCase(), null);
            if (server == null) server = ProxyServer.getInstance().getServerInfo(name);
            if (server != null) {
                boolean valid = true;
                int confidence = 0;
                if (server instanceof ServerContainer) {
                    if (!((ServerContainer) server).isHidden()) confidence++;
                    if (!((ServerContainer) server).isRestricted()) confidence++;
                    if (((ServerContainer) server).getSubData() != null) confidence++;
                } if (server instanceof SubServerContainer) {
                    if (!((SubServerContainer) server).isRunning()) valid = false;
                }

                List<FallbackInspector> inspectors = new ArrayList<FallbackInspector>();
                inspectors.addAll(SmartReconnectHandler.inspectors);
                for (FallbackInspector inspector : inspectors) try {
                    Integer response = inspector.inspect(server);
                    if (response == null) {
                        valid = false;
                    } else {
                        confidence += response;
                    }
                } catch (Throwable e) {
                    new InvocationTargetException(e, "Exception while running inspecting fallback server: " + server.getName()).printStackTrace();
                }

                if (valid) {
                    List<ServerInfo> servers = (score.keySet().contains(confidence))?score.get(confidence):new LinkedList<ServerInfo>();
                    servers.add(server);
                    score.put(confidence, servers);
                }
            }
        }

        Random random = new Random();
        LinkedHashMap<String, ServerInfo> map = new LinkedHashMap<String, ServerInfo>();
        for (List<ServerInfo> servers : score.values()) {
            while (!servers.isEmpty()) {
                ServerInfo next = servers.get(random.nextInt(servers.size()));
                map.put(next.getName(), next);
                servers.remove(next);
            }
        }
        return map;
    }

    /**
     * Add a Fallback Server Inspector
     *
     * @param inspector Inspector
     */
    public static void addInspector(FallbackInspector inspector) {
        if (Util.isNull(inspector)) throw new NullPointerException();
        inspectors.add(inspector);
    }

    /**
     * Remove a Fallback Server Inspector
     *
     * @param inspector Inspector
     */
    public static void removeInspector(FallbackInspector inspector) {
        if (Util.isNull(inspector)) throw new NullPointerException();
        Util.isException(() -> inspectors.remove(inspector));
    }

    @Override
    public void setServer(ProxiedPlayer player) {
        // Ignore server switching
    }

    @Override
    public void save() {
        // Nothing to save
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
