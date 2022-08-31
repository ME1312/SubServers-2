package net.ME1312.SubServers.Web.Library;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Web.SubAPI;

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
        Version now = SubAPI.getInstance().getAppInfo().getBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!existing.contains("Settings") || !existing.getMap("Settings").contains("Version")) {

            i++;
            SubAPI.getInstance().getAppInfo().getLogger().info.println("Created ./config.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                if (existing.getMap("Settings", new YAMLSection()).contains("Log")) {
                    updated.getMap("Settings").safeSet("Console-Log", existing.getMap("Settings").getBoolean("Log"));
                    updated.getMap("Settings").safeSet("Network-Log", existing.getMap("Settings").getBoolean("Log"));
                }

                existing = updated.clone();
                i++;
            } if (was.compareTo(new Version("20w24c")) <= 0) {
              // additions only this time

              i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++;
            //}

            if (i > 0) SubAPI.getInstance().getAppInfo().getLogger().info.println("Updated ./config.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("Console-Log", updated.getMap("Settings", new YAMLSection()).getBoolean("Console-Log", true));
            settings.set("Network-Log", updated.getMap("Settings", new YAMLSection()).getBoolean("Network-Log", true));
            settings.set("Web-Bind", updated.getMap("Settings", new YAMLSection()).getString("Web-Bind", "127.0.0.1"));
            settings.set("Web-Port", updated.getMap("Settings", new YAMLSection()).getInt("Web-Port", 8090));


            YAMLSection subdata = new YAMLSection();
            subdata.set("Name", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getString("Name", "undefined"));
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
