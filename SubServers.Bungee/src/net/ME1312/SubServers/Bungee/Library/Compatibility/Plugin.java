package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Library.Callback.ExceptionRunnable;
import net.ME1312.Galaxi.Library.Util;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginDescription;

import java.io.File;

public final class Plugin extends net.md_5.bungee.api.plugin.Plugin {
    private static final PluginDescription description = new PluginDescription();
    private final ExceptionRunnable enable;
    private final Runnable disable;
    private boolean enabled;

    @Deprecated
    public Plugin() {
        enable = null;
        disable = null;
    }

    private static PluginDescription describe() {
        description.setName("SubServers-Bungee");
        description.setMain(Plugin.class.getCanonicalName());
        description.setFile(Util.getDespiteException(() -> new File(Plugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()), null));
        description.setVersion(net.ME1312.SubServers.Bungee.SubProxy.version.toString());
        description.setAuthor("ME1312");
        return description;
    }

    public Plugin(ProxyServer proxy, ExceptionRunnable enable, Runnable disable) {
        super(proxy, describe());
        this.enable = enable;
        this.disable = disable;

        // 2020 BungeeCord builds don't run init(), but future builds may uncomment that line. We wouldn't want to repeat ourselves.
        if (getDescription() == null) Util.isException(() -> Util.reflect(net.md_5.bungee.api.plugin.Plugin.class.getDeclaredMethod("init", ProxyServer.class, PluginDescription.class), this, proxy, description));
    }

    @Override
    public void onEnable() {
        if (enable == null) {
            throw new IllegalStateException("SubServers.Bungee does not run as a plugin, but a wrapper. For more information on how to install, please visit this page: https://github.com/ME1312/SubServers-2/wiki/Install");
        } else try {
            enabled = true;
            enable.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean isActive() {
        return enabled;
    }

    @Override
    public void onDisable() {
        if (disable != null) disable.run();
    }
}
