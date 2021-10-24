package net.ME1312.SubServers.Sync.Library;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Sync.SubAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
            } if (was.compareTo(new Version("20w34a")) <= 0) {
                if (existing.getMap("Settings", new YAMLSection()).contains("Smart-Fallback") && existing.getMap("Settings").isBoolean("Smart-Fallback")) {
                    YAMLSection smart_fallback = new YAMLSection();
                    smart_fallback.set("Enabled", existing.getMap("Settings").getBoolean("Smart-Fallback"));
                    smart_fallback.set("Fallback", existing.getMap("Settings").getBoolean("Smart-Fallback"));
                    updated.getMap("Settings").set("Smart-Fallback", smart_fallback);
                }
                if (existing.getMap("Settings", new YAMLSection()).contains("Override-Bungee-Commands") && existing.getMap("Settings").isBoolean("Override-Bungee-Commands")) {
                    List<String> overrides = new LinkedList<>();
                    if (!existing.getMap("Settings").getBoolean("Override-Bungee-Commands")) {
                        overrides.add("/server");
                        overrides.add("/glist");
                    }
                    updated.getMap("Settings").set("Disabled-Overrides", overrides);
                }

                existing = updated.clone();
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
            if (updated.getMap("Settings", new YAMLSection()).contains("RPEC-Check-Interval")) settings.set("RPEC-Check-Interval", updated.getMap("Settings").getString("RPEC-Check-Interval"));
            settings.set("Disabled-Overrides", updated.getMap("Settings", new YAMLSection()).getStringList("Disabled-Overrides", Collections.emptyList()));

            YAMLSection smart_fallback = new YAMLSection();
            smart_fallback.set("Enabled", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Enabled", true));
            smart_fallback.set("Fallback", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Fallback", true));
            smart_fallback.set("Reconnect", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Reconnect", false));
            smart_fallback.set("DNS-Forward", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("DNS-Forward", false));
            settings.set("Smart-Fallback", smart_fallback);

            YAMLSection upnp = new YAMLSection();
            upnp.set("Forward-Proxy", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true));
            settings.set("UPnP", upnp);

            YAMLSection subdata = new YAMLSection();
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Name")) subdata.set("Name", updated.getMap("Settings").getMap("SubData").getString("Name"));
            subdata.set("Address", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getString("Address", "127.0.0.1:4391"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Password")) subdata.set("Password", updated.getMap("Settings").getMap("SubData").getString("Password"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Reconnect")) subdata.set("Reconnect", updated.getMap("Settings").getMap("SubData").getInt("Reconnect"));
            settings.set("SubData", subdata);

            rewritten.set("Settings", settings);


            YAMLSection sync = new YAMLSection();
            sync.set("Disabled-Commands", updated.getMap("Settings", new YAMLSection()).getBoolean("Disabled-Commands", false));
            sync.set("Forced-Hosts", updated.getMap("Settings", new YAMLSection()).getBoolean("Forced-Hosts", true));
            sync.set("Motd", updated.getMap("Settings", new YAMLSection()).getBoolean("Motd", false));
            sync.set("Player-Limit", updated.getMap("Settings", new YAMLSection()).getBoolean("Player-Limit", false));
            sync.set("Server-Priorities", updated.getMap("Settings", new YAMLSection()).getBoolean("Server-Priorities", true));

            rewritten.set("Sync", sync);

            config.set(rewritten);
            config.save();
        }
    }
}
