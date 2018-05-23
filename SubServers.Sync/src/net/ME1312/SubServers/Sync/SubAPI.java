package net.ME1312.SubServers.Sync;

import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.UniversalFile;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.SubDataClient;
import net.ME1312.SubServers.Sync.Server.Server;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    LinkedList<NamedContainer<Runnable, Runnable>> listeners = new LinkedList<NamedContainer<Runnable, Runnable>>();
    private final SubPlugin plugin;
    private static SubAPI api;

    protected SubAPI(SubPlugin plugin) {
        this.plugin = plugin;
        api = this;
    }

    /**
     * Gets the SubAPI Methods
     *
     * @return SubAPI
     */
    public static SubAPI getInstance() {
        return api;
    }

    /**
     * Gets the SubServers Internals
     *
     * @deprecated Use SubAPI Methods when available
     * @return SubPlugin Internals
     */
    @Deprecated
    public SubPlugin getInternals() {
        return plugin;
    }

    /**
     * Adds a SubAPI Listener
     *
     * @param enable An Event that will be called when SubAPI is ready
     * @param disable An Event that will be called before SubAPI is disabled
     */
    public void addListener(Runnable enable, Runnable disable) {
        listeners.add(new NamedContainer<Runnable, Runnable>(enable, disable));
    }

    /**
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public SubDataClient getSubDataNetwork() {
        return plugin.subdata;
    }

    /**
     * Gets the Servers (including SubServers)
     *
     * @return Server Map
     */
    public Map<String, Server> getServers() {
        return plugin.servers;
    }

    /**
     * Gets a Server
     *
     * @param name Server name
     * @return a Server
     */
    public Server getServer(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getServers().get(name.toLowerCase());
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return plugin.lang.get().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        if (Util.isNull(channel)) throw new NullPointerException();
        return new LinkedHashMap<>(plugin.lang.get().get(channel.toLowerCase()));
    }

    /**
     * Gets a value from the SubServers Lang
     *
     * @param channel Lang Channel
     * @param key Key
     * @return Lang Values
     */
    public String getLang(String channel, String key) {
        if (Util.isNull(channel, key)) throw new NullPointerException();
        return getLang(channel).get(key);
    }

    /**
     * Gets the Runtime Directory
     *
     * @return Directory
     */
    public UniversalFile getRuntimeDirectory() {
        return plugin.dir;
    }

    /**
     * Gets the SubServers.Sync Version
     *
     * @return SubServers.Sync Version
     */
    public Version getWrapperVersion() {
        return plugin.version;
    }

    /**
     * Gets the BungeeCord Version
     *
     * @return BungeeCord Version
     */
    public Version getProxyVersion() {
        return new Version(plugin.getVersion());
    }

    /**
     * Gets the Latest Supported Minecraft Version
     *
     * @return Minecraft Version
     */
    public Version getGameVersion() {
        if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
            return new Version(System.getProperty("subservers.minecraft.version"));
        } else {
            String raw = plugin.getGameVersion();
            if (raw == null) {
                if (System.getProperty("subservers.minecraft.version.unknown", "false").equalsIgnoreCase("false")) {
                    System.setProperty("subservers.minecraft.version.unknown", "true");
                    System.out.println("Could not determine compatible Minecraft version(s); Now using 1.x.x as a placeholder.");
                    System.out.println("Use this launch argument to specify a compatible Minecraft version: -Dsubservers.minecraft.version=1.x.x");
                }
                return new Version("1.x.x");
            } else if (raw.contains(",")) {
                String[] split = raw.split(",\\s*");
                return new Version(split[split.length - 1]);
            } else if (raw.contains("-")) {
                String[] split = raw.split("\\s*-\\s*");
                return new Version(split[split.length - 1]);
            } else {
                return new Version(plugin.getGameVersion());
            }
        }
    }
}
