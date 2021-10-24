package net.ME1312.SubServers.Client.Bukkit.Library;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.SubAPI;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * SubServers Configuration Updater
 */
public class ConfigUpdater {
    private static final Version UNSIGNED = new Version(new SimpleDateFormat("yy'w'ww'zz'").format(Calendar.getInstance().getTime()));

    /**
     * Update SubServers' config.yml
     *
     * @param file File to bring up-to-date
     */
    public static void updateConfig(File file) throws IOException {
        YAMLConfig config = new YAMLConfig(file);
        YAMLSection existing = config.get().clone();
        YAMLSection updated = existing.clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = existing.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getPluginBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!existing.contains("Settings") || !existing.getMap("Settings").contains("Version")) {

            i++;
            Bukkit.getLogger().info("SubServers > Created ./plugins/SubServers-Client-Bukkit/config.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                if (existing.getMap("Settings", new YAMLSection()).contains("Ingame-Access"))
                    updated.getMap("Settings").safeSet("API-Only-Mode", !existing.getMap("Settings").getBoolean("Ingame-Access"));

                existing = updated.clone();
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) Bukkit.getLogger().info("SubServers > Updated ./plugins/SubServers-Client-Bukkit/config.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("API-Only-Mode", updated.getMap("Settings", new YAMLSection()).getBoolean("API-Only-Mode", false));
            settings.set("Allow-Deletion", updated.getMap("Settings", new YAMLSection()).getBoolean("Allow-Deletion", false));
            settings.set("Show-Addresses", updated.getMap("Settings", new YAMLSection()).getBoolean("Show-Addresses", false));
            settings.set("Use-Title-Messages", updated.getMap("Settings", new YAMLSection()).getBoolean("Use-Title-Messages", true));
            settings.set("PlaceholderAPI-Ready", updated.getMap("Settings", new YAMLSection()).getBoolean("PlaceholderAPI-Ready", false));
            if (updated.getMap("Settings", new YAMLSection()).contains("PlaceholderAPI-Cache-Interval")) settings.set("PlaceholderAPI-Cache-Interval", updated.getMap("Settings").getInt("PlaceholderAPI-Cache-Interval"));
            if (updated.getMap("Settings", new YAMLSection()).contains("Connect-Address")) settings.set("Connect-Address", updated.getMap("Settings").getString("Connect-Address"));

            YAMLSection subdata = new YAMLSection();
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Name")) subdata.set("Name", updated.getMap("Settings").getMap("SubData").getString("Name"));
            subdata.set("Address", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getString("Address", "127.0.0.1:4391"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Password")) subdata.set("Password", updated.getMap("Settings").getMap("SubData").getString("Password"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Reconnect")) subdata.set("Reconnect", updated.getMap("Settings").getMap("SubData").getInt("Reconnect"));
            settings.set("SubData", subdata);

            rewritten.set("Settings", settings);

            config.set(rewritten);
            config.save();
        }
    }
}
