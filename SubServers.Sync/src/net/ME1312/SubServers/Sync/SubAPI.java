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
    private SubPlugin plugin;
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
     * Gets a value from the SubServers Lang
     *
     * @param key Key
     * @return Lang Value
     */
    public String getLang(String key) {
        if (Util.isNull(key)) throw new NullPointerException();
        return getLang().get(key);
    }

    /**
     * Gets the SubServers Lang
     *
     * @return SubServers Lang
     */
    public Map<String, String> getLang() {
        HashMap<String, String> lang = new HashMap<String, String>();
        for (String key : plugin.lang.getSection("Lang").getKeys()) {
            if (plugin.lang.getSection("Lang").isString(key)) lang.put(key, plugin.lang.getSection("Lang").getString(key));
        }
        return lang;
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
     * Gets the SubServers.Sync Beta Version
     *
     * @return SubServers.Sync Beta Version (or null if this is a release version)
     */
    public Version getBetaVersion() {
        return plugin.bversion;
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
            if (raw.contains(",")) {
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
