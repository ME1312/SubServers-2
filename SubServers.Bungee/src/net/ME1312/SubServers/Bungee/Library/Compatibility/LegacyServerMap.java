package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Legacy Server Map Translation Class
 */
public class LegacyServerMap implements Map<String, ServerInfo> {
    private final Map<String, ServerInfo> m;

    /**
     * Translate Legacy Server Map Modifications
     *
     * @param map Legacy Server Map
     */
    public LegacyServerMap(Map<String, ServerInfo> map) {
        this.m = map;
    }

    @Override
    public ServerInfo get(Object key) {
        return m.get(key);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ServerInfo put(String key, ServerInfo value) {
        if (value == null) throw new NullPointerException();
        ServerInfo n = SubAPI.getInstance().addServer(value.getName(), value.getAddress().getAddress(), value.getAddress().getPort(), value.getMotd(), false, value.isRestricted()),
                   s = getOrDefault(key, null);

        if (n != null)
            m.put(n.getName(), n);
        return s;
    }

    @Override
    public ServerInfo remove(Object key) {
        if (key instanceof String) {
            ServerInfo s = getOrDefault(key, null);
            if (s != null) {
                if (SubAPI.getInstance().removeServer((String) key))
                    m.remove(key);
                return s;
            } else return null;
        } else return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends ServerInfo> m) {
        if (m.size() > 0) {
            for (Map.Entry<? extends String, ? extends ServerInfo> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }
    }

    @Override
    public void clear() {
        // Disallow removing all servers
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return m.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return m.containsValue(value);
    }

    @Override
    public Set<String> keySet() {
        return m.keySet();
    }

    @Override
    public Collection<ServerInfo> values() {
        return m.values();
    }

    @Override
    public Set<Entry<String, ServerInfo>> entrySet() {
        return m.entrySet();
    }


}
