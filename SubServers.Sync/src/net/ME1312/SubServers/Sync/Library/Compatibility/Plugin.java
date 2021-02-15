package net.ME1312.SubServers.Sync.Library.Compatibility;

public final class Plugin extends net.md_5.bungee.api.plugin.Plugin {

    @Deprecated
    public Plugin() {
        throw new IllegalStateException("SubServers.Sync does not run as a plugin, but a wrapper. For more information on how to install, please visit this page: https://github.com/ME1312/SubServers-2/wiki/Installation");
    }
}
