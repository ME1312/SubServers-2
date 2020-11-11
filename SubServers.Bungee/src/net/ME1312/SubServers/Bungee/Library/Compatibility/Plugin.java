package net.ME1312.SubServers.Bungee.Library.Compatibility;

import net.ME1312.Galaxi.Library.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginDescription;

import java.io.File;

public final class Plugin extends net.md_5.bungee.api.plugin.Plugin {
    private static final PluginDescription description = new PluginDescription();
    private final boolean invalid;

    @Deprecated
    public Plugin() {
        this.invalid = true;
    }

    private static PluginDescription describe() {
        description.setName("SubServers-Bungee");
        description.setMain(Plugin.class.getCanonicalName());
        description.setFile(Util.getDespiteException(() -> new File(Plugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()), null));
        description.setVersion(net.ME1312.SubServers.Bungee.SubProxy.version.toString());
        description.setAuthor("ME1312");
        return description;
    }

    public Plugin(ProxyServer proxy) {
        super(proxy, describe());
        this.invalid = false;

        // 2020 BungeeCord builds don't run init(), but future builds may uncomment that line. We wouldn't want to repeat ourselves.
        if (getDescription() == null) Util.isException(() -> Util.reflect(net.md_5.bungee.api.plugin.Plugin.class.getDeclaredMethod("init", ProxyServer.class, PluginDescription.class), this, proxy, description));
    }

    @Override
    public void onEnable() {
        if (invalid) throw new IllegalStateException("SubServers.Bungee does not run as a plugin, but a wrapper. For more information on how to install, please visit this page: https://github.com/ME1312/SubServers-2/wiki/Install");
    }
}
