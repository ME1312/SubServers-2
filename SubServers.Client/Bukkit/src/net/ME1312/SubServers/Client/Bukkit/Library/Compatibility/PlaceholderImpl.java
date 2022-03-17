package net.ME1312.SubServers.Client.Bukkit.Library.Compatibility;

import net.ME1312.SubServers.Client.Bukkit.SubPlugin;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI Implementation Class
 */
public class PlaceholderImpl extends PlaceholderExpansion implements Taskable, Cacheable {
    private SubPlugin plugin;

    /**
     * Create a PlaceholderAPI Implementation Instance
     *
     * @param plugin SubPlugin
     */
    public PlaceholderImpl(SubPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "subservers";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public void start() {
        // do nothing
    }

    @Override
    public void stop() {
        plugin.phi.stop();
    }

    @Override
    public void clear() {
        plugin.phi.clear();
    }

    @Override
    public String onPlaceholderRequest(Player player, String request) {
        return onRequest(player, request);
    }

    @Override
    public String onRequest(OfflinePlayer player, String request) {
        return plugin.phi.request(player, request);
    }
}
