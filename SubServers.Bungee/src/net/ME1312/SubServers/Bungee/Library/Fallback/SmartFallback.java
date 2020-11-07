package net.ME1312.SubServers.Bungee.Library.Fallback;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;
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

    public SmartFallback(SubProxy proxy) {
        if (reconnect == null && proxy.config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("Reconnect", false))
            reconnect = Util.getDespiteException(() -> Util.reflect(ProxyServer.getInstance().getPluginManager().getPlugin("reconnect_yaml").getClass().getClassLoader().loadClass("net.md_5.bungee.module.reconnect.yaml.YamlReconnectHandler").getConstructor()), null);
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
        if (connection.getVirtualHost() == null || !((SubProxy) ProxyServer.getInstance()).config.get().getMap("Settings").getMap("Smart-Fallback", new ObjectMap<>()).getBoolean("DNS-Forward", false)) {
            return null;
        } else {
            Map.Entry<String, ServerInfo> server = null;
            String dns = connection.getVirtualHost().getHostString().toLowerCase();
            for (Map.Entry<String, ServerInfo> s : ((SubProxy) ProxyServer.getInstance()).getServersCopy().entrySet()) {
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
            ServerInfo server = SubAPI.getInstance().getServer(name.toLowerCase());
            if (server == null) server = ProxyServer.getInstance().getServerInfo(name);
            if (server != null) {
                boolean valid = true;
                double confidence = 0;
                if (server instanceof Server) {
                    if (!((Server) server).isHidden()) confidence++;
                    if (!((Server) server).isRestricted()) confidence++;
                    if (((Server) server).getSubData()[0] != null) confidence++;

                    if (player != null) {
                        if (((Server) server).canAccess(player)) confidence++;
                    }
                } if (server instanceof SubServer) {
                    if (!((SubServer) server).isRunning()) valid = false;
                }

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
