package net.ME1312.SubServers.Sync.Library.Updates;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Sync.Library.Compatibility.Logger;
import net.ME1312.SubServers.Sync.SubAPI;

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
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!existing.contains("Settings") || !existing.getMap("Settings").contains("Version")) {

            i++;
            Logger.get("SubServers").info("Created ./SubServers/sync.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {

                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) Logger.get("SubServers").info("Updated ./SubServers/sync.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("Smart-Fallback", updated.getMap("Settings", new YAMLSection()).getBoolean("Smart-Fallback", true));
            settings.set("Override-Bungee-Commands", updated.getMap("Settings", new YAMLSection()).getBoolean("Override-Bungee-Commands", true));

            YAMLSection upnp = new YAMLSection();
            upnp.set("Forward-Proxy", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true));
            settings.set("UPnP", upnp);

            YAMLSection subdata = new YAMLSection();
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Name")) subdata.set("Name", updated.getMap("Settings").getMap("SubData").getRawString("Name"));
            subdata.set("Address", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Address", "127.0.0.1:4391"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Password")) subdata.set("Password", updated.getMap("Settings").getMap("SubData").getRawString("Password"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Reconnect")) subdata.set("Reconnect", updated.getMap("Settings").getMap("SubData").getInt("Reconnect"));
            settings.set("SubData", subdata);

            rewritten.set("Settings", settings);


            YAMLSection sync = new YAMLSection();
            settings.set("Disabled-Commands", updated.getMap("Settings", new YAMLSection()).getBoolean("Disabled-Commands", false));
            settings.set("Forced-Hosts", updated.getMap("Settings", new YAMLSection()).getBoolean("Forced-Hosts", true));
            settings.set("Motd", updated.getMap("Settings", new YAMLSection()).getBoolean("Motd", false));
            settings.set("Player-Limit", updated.getMap("Settings", new YAMLSection()).getBoolean("Player-Limit", false));
            settings.set("Server-Priorities", updated.getMap("Settings", new YAMLSection()).getBoolean("Server-Priorities", true));

            rewritten.set("Sync", sync);

            config.set(rewritten);
            config.save();
        }
    }
}
