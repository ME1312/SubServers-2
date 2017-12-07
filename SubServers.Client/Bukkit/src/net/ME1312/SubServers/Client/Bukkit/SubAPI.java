package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * SubAPI Class
 */
public final class SubAPI {
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
     * Gets the SubData Network Manager
     *
     * @return SubData Network Manager
     */
    public SubDataClient getSubDataNetwork() {
        return plugin.subdata;
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
     * Gets the SubServers Beta Version
     *
     * @return SubServers Beta Version (or null if this is a release version)
     */
    public Version getBetaVersion() {
        return plugin.bversion;
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
        try {
            return new Version(Bukkit.getBukkitVersion().split("-")[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return new Version(plugin.version.toString().substring(0, plugin.version.toString().length() - 1));
        }
    }
}
