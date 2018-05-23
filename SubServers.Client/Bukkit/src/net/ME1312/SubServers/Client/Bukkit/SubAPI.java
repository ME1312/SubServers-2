package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.Bukkit;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI {
    LinkedList<Runnable> reloadListeners = new LinkedList<Runnable>();
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
     * Adds a SubAPI Reload Listener
     *
     * @param reload An Event that will be called after SubAPI is soft-reloaded
     */
    public void addListener(Runnable reload) {
        if (reload != null) reloadListeners.add(reload);
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
     * Gets the Graphics Handler
     *
     * @return Graphics Handler
     */
    public UIHandler getGraphicHandler() {
        return plugin.gui;
    }

    /**
     * Sets the Graphics Handler for SubServers to use
     *
     * @param graphics Graphics Handler
     */
    public void setGraphicHandler(UIHandler graphics) {
        if (Util.isNull(graphics)) throw new NullPointerException();
        plugin.gui.disable();
        plugin.gui = graphics;
    }

    /**
     * Gets the SubServers Version
     *
     * @return SubServers Version
     */
    public Version getPluginVersion() {
        return plugin.version;
    }

    /**
     * Gets the Server Version
     *
     * @return Server Version
     */
    public Version getServerVersion() {
        return new Version(Bukkit.getServer().getVersion());
    }

    /**
     * Gets the Minecraft Version
     *
     * @return Minecraft Version
     */
    public Version getGameVersion() {
        if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
            return new Version(System.getProperty("subservers.minecraft.version"));
        } else {
            try {
                return new Version(Bukkit.getBukkitVersion().split("-")[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                if (System.getProperty("subservers.minecraft.version.unknown", "false").equalsIgnoreCase("false")) {
                    System.setProperty("subservers.minecraft.version.unknown", "true");
                    System.out.println("Could not determine this server's game version; Now using 1.x.x as a placeholder.");
                    System.out.println("Use this launch argument to specify what version this server serves: -Dsubservers.minecraft.version=1.x.x");
                }
                return new Version("1.x.x");
            }
        }
    }
}
