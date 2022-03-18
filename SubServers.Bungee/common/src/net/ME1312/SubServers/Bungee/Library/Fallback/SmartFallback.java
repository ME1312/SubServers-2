package net.ME1312.SubServers.Bungee.Library.Fallback;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.BungeeCommon;

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

/**
 * Smart Fallback Handler Class
 */
public class SmartFallback implements ReconnectHandler {
    private static List<FallbackInspector> inspectors = new CopyOnWriteArrayList<FallbackInspector>();
    private static ReconnectHandler reconnect;
    public static boolean dns_forward = false;

    public SmartFallback(ObjectMap<String> settings) {
        dns_forward = settings.getBoolean("DNS-Forward", false);
        if (reconnect == null && settings.getBoolean("Reconnect", false))
            reconnect = Try.all.get(() -> Util.reflect(ProxyServer.getInstance().getPluginManager().getPlugin("reconnect_yaml").getClass().getClassLoader().loadClass("net.md_5.bungee.module.reconnect.yaml.YamlReconnectHandler").getConstructor()));
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        return getServer(player, player instanceof UserConnection);
    }

    protected ServerInfo getServer(ProxiedPlayer player, boolean queue) {
        ServerInfo override;
        if ((override = getForcedHost(player.getPendingConnection())) != null
                || (override = getDNS(player.getPendingConnection())) != null) {
            if (queue) ((UserConnection) player).setServerJoinQueue(new LinkedList<>());
            return override;
        } else {
            Map<String, ServerInfo> fallbacks = getFallbackServers(player.getPendingConnection().getListener(), player);

            if ((override = getReconnectServer(player)) != null || !fallbacks.isEmpty()) {
                if (queue) ((UserConnection) player).setServerJoinQueue(new LinkedList<>(fallbacks.keySet()));
                return (override != null)? override : new LinkedList<>(fallbacks.values()).getFirst();
            } else {
                return null;
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
     * Grabs the Server that a connection's DNS matches
     *
     * @param connection Connection to check
     * @return DNS Forward Server
     */
    public static ServerInfo getDNS(PendingConnection connection) {
        if (connection.getVirtualHost() == null || !dns_forward) {
            return null;
        } else {
            Map.Entry<String, ServerInfo> server = null;
            String dns = connection.getVirtualHost().getHostString().toLowerCase();
            for (Map.Entry<String, ServerInfo> s : ((BungeeCommon) ProxyServer.getInstance()).getServersCopy().entrySet()) {
                if (dns.startsWith(s.getKey().toLowerCase() + '.'))
                    if (server == null || server.getKey().length() < s.getKey().length())
                        server = s;
            }

            return (server == null)?null:server.getValue();
        }
    }

    /**
     * Grabs the Server that a player was last connected to
     *
     * @param player Player
     * @return Reconnect Server
     */
    public static ServerInfo getReconnectServer(ProxiedPlayer player) {
        if (reconnect == null) {
            return null;
        } else try {
            return Util.reflect(reconnect.getClass().getDeclaredMethod("getStoredServer", ProxiedPlayer.class), reconnect, player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a <i>smart</i> sorted map of fallback servers using a generated confidence score
     *
     * @param listener Listener to grab fallback servers from
     * @return Fallback Server Map (with legacy bungee case-sensitive keys)
     */
    public static Map<String, ServerInfo> getFallbackServers(ListenerInfo listener) {
        return getFallbackServers(listener, null);
    }

    /**
     * Generates a <i>smart</i> sorted map of fallback servers using a generated confidence score
     *
     * @param listener Listener to grab fallback servers from
     * @param player Player that is requesting fallback servers
     * @return Fallback Server Map (with legacy bungee case-sensitive keys)
     */
    public static Map<String, ServerInfo> getFallbackServers(ListenerInfo listener, ProxiedPlayer player) {
        TreeMap<Double, List<ServerInfo>> score = new TreeMap<Double, List<ServerInfo>>(Collections.reverseOrder());
        for (String name : listener.getServerPriority()) {
            ServerInfo server = ProxyServer.getInstance().getServerInfo(name);
            if (server != null) {
                boolean valid = true;
                double confidence = 0;

                List<FallbackInspector> inspectors = new ArrayList<FallbackInspector>();
                inspectors.addAll(SmartFallback.inspectors);
                for (FallbackInspector inspector : inspectors) try {
                    Double response = inspector.inspect(player, server);
                    if (response == null) {
                        valid = false;
                    } else {
                        confidence += response;
                    }
                } catch (Throwable e) {
                    new InvocationTargetException(e, "Exception while running inspecting fallback server: " + server.getName()).printStackTrace();
                }

                if (valid) {
                    List<ServerInfo> servers = (score.containsKey(confidence))?score.get(confidence):new LinkedList<ServerInfo>();
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
        Util.nullpo(inspector);
        inspectors.add(inspector);
    }

    /**
     * Remove a Fallback Server Inspector
     *
     * @param inspector Inspector
     */
    public static void removeInspector(FallbackInspector inspector) {
        Util.nullpo(inspector);
        Try.all.run(() -> inspectors.remove(inspector));
    }

    @Override
    public void setServer(ProxiedPlayer player) {
        if (reconnect != null) reconnect.setServer(player);
    }

    @Override
    public void save() {
        if (reconnect != null) reconnect.save();
    }

    @Override
    public void close() {
        if (reconnect != null) reconnect.close();
    }
}
