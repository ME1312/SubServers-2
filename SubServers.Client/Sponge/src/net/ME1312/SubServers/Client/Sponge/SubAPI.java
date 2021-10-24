package net.ME1312.SubServers.Client.Sponge;

import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubServers.Client.Common.ClientAPI;
import net.ME1312.SubServers.Client.Sponge.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Sponge.Library.AccessMode;

import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.*;

/**
 * SubAPI Class
 */
public final class SubAPI extends ClientAPI {
    LinkedList<Runnable> reloadListeners = new LinkedList<Runnable>();
    private final SubPlugin plugin;
    private static SubAPI api;
    AccessMode access;
    String name;

    SubAPI(SubPlugin plugin) {
        this.plugin = plugin;
        GAME_VERSION = getGameVersion();
        access = AccessMode.DEFAULT;
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
     * Get the Server Name
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the SubData Network Connections
     *
     * @return SubData Network Connections
     */
    public DataClient[] getSubDataNetwork() {
        Integer[] keys = plugin.subdata.keySet().toArray(new Integer[0]);
        DataClient[] channels = new DataClient[keys.length];
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; ++i) channels[i] = plugin.subdata.get(keys[i]);
        return channels;
    }

    /**
     * Gets the SubData Network Protocol
     *
     * @return SubData Network Protocol
     */
    public DataProtocol getSubDataProtocol() {
        return plugin.subprotocol;
    }

    /**
     * Gets the current SubServers Lang Channels
     *
     * @return SubServers Lang Channel list
     */
    public Collection<String> getLangChannels() {
        return plugin.lang.value().keySet();
    }

    /**
     * Gets values from the SubServers Lang
     *
     * @param channel Lang Channel
     * @return Lang Value
     */
    public Map<String, String> getLang(String channel) {
        Util.nullpo(channel);
        return new LinkedHashMap<>(plugin.lang.value().get(channel.toLowerCase()));
    }

    /**
     * Get the plugin's access mode
     *
     * @return Access Mode
     */
    public AccessMode getAccessMode() {
        return access;
    }

    /**
     * Set the plugin's access mode
     *
     * @param mode Access Mode
     */
    public void setAccessMode(AccessMode mode) {
        if (mode.value <= access.value && !plugin.running) {
            access = mode;
        }
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
        if (plugin.gui != null) plugin.gui.disable();
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
     * Gets the SubServers Build Version
     *
     * @return SubServers Build Version (or null if unsigned)
     */
    public Version getPluginBuild() {
        return (SubPlugin.class.getPackage().getSpecificationTitle() != null)?new Version(SubPlugin.class.getPackage().getSpecificationTitle()):null;
    }

    /**
     * Gets the Server Version
     *
     * @return Server Version
     */
    public Version getServerVersion() {
        PluginContainer container =        Try.all.get(() -> (PluginContainer) Platform.class.getMethod("getValue", Class.forName("org.spongepowered.api.Platform$Component")).invoke(Sponge.getPlatform(), Enum.valueOf((Class<Enum>) Class.forName("org.spongepowered.api.Platform$Component"), "IMPLEMENTATION")));
        if (container == null) container = Try.all.get(() -> (PluginContainer) Platform.class.getMethod("getImplementation").invoke(Sponge.getPlatform()));
        return (container == null || !container.getVersion().isPresent())?null:new Version(container.getVersion().get());
    }

    /**
     * Gets the Minecraft Version
     *
     * @return Minecraft Version
     */
    public Version getGameVersion() {
        if (GAME_VERSION == null) {
            if (System.getProperty("subservers.minecraft.version", "").length() > 0) {
                return new Version(System.getProperty("subservers.minecraft.version"));
            } else {
                return new Version(plugin.game.getPlatform().getMinecraftVersion().getName());
            }
        } else return GAME_VERSION;
    }
    private final Version GAME_VERSION;
}
