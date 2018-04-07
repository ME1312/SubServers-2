package net.ME1312.SubServers.Bungee.Library.Compatibility;

public class Plugin extends net.md_5.bungee.api.plugin.Plugin {

    @Override
    public void onEnable() {
        throw new IllegalStateException("SubServers.Bungee does not run as a plugin, but a wrapper. For more information on how to install, visit this page: https://github.com/ME1312/SubServers-2/wiki/Install");
    }
}
