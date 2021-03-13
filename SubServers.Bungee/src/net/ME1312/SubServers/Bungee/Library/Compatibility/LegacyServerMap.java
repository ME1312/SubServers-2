package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.SubServers.Bungee.SubAPI;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.util.CaseInsensitiveMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Legacy Server Map Translation Class
 */
public class LegacyServerMap extends CaseInsensitiveMap<ServerInfo> {
    public LegacyServerMap() {

    }

    public LegacyServerMap(Map<String, ServerInfo> map) {
        for (Entry<String, ServerInfo> e : map.entrySet()) super.put(e.getKey(), e.getValue());
    }

    @SuppressWarnings("deprecation")
    @Override
    public ServerInfo put(String key, ServerInfo value) {
        if (value == null) throw new NullPointerException();
        ServerInfo n = SubAPI.getInstance().addServer(value.getName(), value.getAddress().getAddress(), value.getAddress().getPort(), value.getMotd(), false, value.isRestricted()),
                   s = getOrDefault(key, null);

        if (n != null)
            super.put(n.getName(), n);
        return s;
    }

    @Override
    public ServerInfo remove(Object key) {
        if (key instanceof String) {
            ServerInfo s = getOrDefault(key, null);
            if (s != null) {
                if (SubAPI.getInstance().removeServer((String) key))
                    super.remove(key);
                return s;
            } else return null;
        } else return null;
    }

    @Override
    public void clear() {
        // Disallow removing all servers
    }
}
