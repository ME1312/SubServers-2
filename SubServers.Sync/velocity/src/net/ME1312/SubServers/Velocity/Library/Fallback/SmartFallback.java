package net.ME1312.SubServers.Velocity.Library.Fallback;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Velocity.ExProxy;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Smart Fallback Handler Class
 */
public class SmartFallback {
    private static List<FallbackInspector> inspectors = new CopyOnWriteArrayList<FallbackInspector>();
    public static boolean dns_forward = false;

    public SmartFallback(ObjectMap<String> settings) {
        dns_forward = settings.getBoolean("DNS-Forward", false);
    }

    @Subscribe
    public void getServer(PlayerChooseInitialServerEvent e) {
        RegisteredServer[] overrides;
        if ((overrides = getForcedHosts(e.getPlayer())) != null
                || (overrides = getDNS(e.getPlayer())) != null) {
            e.setInitialServer(overrides[0]);
        } else {
            Map<String, RegisteredServer> fallbacks = getFallbackServers(e.getPlayer());

            if (/*(override = getReconnectServer(player)) != null || */!fallbacks.isEmpty()) {
                e.setInitialServer((overrides != null)? overrides[0] : new LinkedList<>(fallbacks.values()).getFirst());
            } else {
                e.setInitialServer(null);
            }
        }
    }

    /**
     * Grabs the Forced Host Server for this connection
     *
     * @param connection Connection to check
     * @return Forced Host Servers (or null if there are none)
     */
    public static RegisteredServer[] getForcedHosts(InboundConnection connection) {
        if (!connection.getVirtualHost().isPresent()) {
            return null;
        } else {
            List<String> names = ExProxy.getInstance().getConfiguration().getForcedHosts().get(connection.getVirtualHost().get().getHostString());
            if (names == null) {
                return null;
            } else {
                LinkedList<RegisteredServer> forced = new LinkedList<>();
                for (String name : names) {
                    Optional<RegisteredServer> server = ExProxy.getInstance().getServer(name);
                    server.ifPresent(forced::add);
                }
                return (forced.size() == 0)? null : forced.toArray(new RegisteredServer[0]);
            }
        }
    }

    /**
     * Grabs the Server that a connection's DNS matches
     *
     * @param connection Connection to check
     * @return DNS Forward Server
     */
    public static RegisteredServer[] getDNS(InboundConnection connection) {
        if (!connection.getVirtualHost().isPresent() || !dns_forward) {
            return null;
        } else {
            RegisteredServer server = null;
            String dns = connection.getVirtualHost().get().getHostString().toLowerCase();
            for (RegisteredServer s : ExProxy.getInstance().getAllServers()) {
                if (dns.startsWith(s.getServerInfo().getName().toLowerCase() + '.'))
                    if (server == null || server.getServerInfo().getName().length() < s.getServerInfo().getName().length())
                        server = s;
            }

            return (server == null)? null : new RegisteredServer[]{ server };
        }
    }

    /**
     * Grabs the Server that a player was last connected to
     *
     * @param player Player
     * @return Reconnect Server
     *//*
    public static ServerInfo getReconnectServer(Player player) {
        if (reconnect == null) {
            return null;
        } else try {
            return Util.reflect(reconnect.getClass().getDeclaredMethod("getStoredServer", ProxiedPlayer.class), reconnect, player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    } */

    /**
     * Generates a <i>smart</i> sorted map of fallback servers using a generated confidence score
     *
     * @return Fallback Server Map (with legacy bungee case-sensitive keys)
     */
    public static Map<String, RegisteredServer> getFallbackServers() {
        return getFallbackServers(null);
    }

    /**
     * Generates a <i>smart</i> sorted map of fallback servers using a generated confidence score
     *
     * @param player Player that is requesting fallback servers
     * @return Fallback Server Map (with legacy bungee case-sensitive keys)
     */
    public static Map<String, RegisteredServer> getFallbackServers(Player player) {
        TreeMap<Double, List<RegisteredServer>> score = new TreeMap<Double, List<RegisteredServer>>(Collections.reverseOrder());
        for (String name : ExProxy.getInstance().getConfiguration().getAttemptConnectionOrder()) {
            RegisteredServer server = ExProxy.getInstance().getServer(name).orElse(null);
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
                    new InvocationTargetException(e, "Exception while running inspecting fallback server: " + server.getServerInfo().getName()).printStackTrace();
                }

                if (valid) {
                    List<RegisteredServer> servers = (score.keySet().contains(confidence))?score.get(confidence):new LinkedList<RegisteredServer>();
                    servers.add(server);
                    score.put(confidence, servers);
                }
            }
        }

        Random random = new Random();
        LinkedHashMap<String, RegisteredServer> map = new LinkedHashMap<String, RegisteredServer>();
        for (List<RegisteredServer> servers : score.values()) {
            while (!servers.isEmpty()) {
                RegisteredServer next = servers.get(random.nextInt(servers.size()));
                map.put(next.getServerInfo().getName(), next);
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
}
