package net.ME1312.SubServers.Bungee.Library.Compatibility.Updates;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;

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
        YAMLSection updated = config.get().clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = updated.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!updated.contains("Settings") || !updated.getMap("Settings").contains("Version")) {
            YAMLSection hosts = new YAMLSection();
            YAMLSection host = new YAMLSection();
            host.set("Enabled", true);
            host.set("Display", "Default");
            hosts.set("~", host);
            updated.set("Hosts", hosts);

            i++;
            System.out.println("SubServers > Created ./SubServers/config.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                if (updated.getMap("Settings", new YAMLSection()).contains("Log-Creator")) for (String name : updated.getMap("Hosts", new YAMLSection()).getKeys())
                    updated.getMap("Hosts").getMap(name).safeSet("Log-Creator", updated.getMap("Settings").getBoolean("Log-Creator"));

                if (updated.getMap("Settings", new YAMLSection()).contains("SubData") && !updated.getMap("Settings", new YAMLSection()).getMap("SubData").contains("Encryption"))
                    updated.getMap("Settings").getMap("SubData").set("Encryption", "NONE");

                if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Allowed-Connections"))
                    updated.getMap("Settings").getMap("SubData").safeSet("Whitelist", updated.getMap("Settings").getMap("SubData").getRawStringList("Allowed-Connections"));

                if (updated.contains("Servers")) {
                    YAMLConfig sc = new YAMLConfig(new File(file.getParentFile(), "servers.yml"));
                    YAMLSection settings = new YAMLSection();
                    settings.set("Version", was.toString());
                    settings.set("Run-On-Launch-Timeout", (updated.getMap("Settings", new YAMLSection()).contains("Run-On-Launch-Timeout"))?updated.getMap("Settings").getInt("Run-On-Launch-Timeout"):0);
                    sc.get().safeSet("Settings", settings);

                    sc.get().safeSet("Servers", new YAMLSection());
                    sc.get().getMap("Servers").safeSetAll(updated.getMap("Servers"));
                    System.out.println("SubServers > Created ./SubServers/servers.yml (using existing data)");
                    sc.save();
                }
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) System.out.println("SubServers > Updated ./SubServers/config.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("Override-Bungee-Commands", updated.getMap("Settings", new YAMLSection()).getBoolean("Override-Bungee-Commands", true));

            YAMLSection upnp = new YAMLSection();
            upnp.set("Forward-Proxy", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true));
            upnp.set("Forward-SubData", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-SubData", false));
            upnp.set("Forward-Servers", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Servers", false));
            settings.set("UPnP", upnp);

            YAMLSection subdata = new YAMLSection();
            subdata.set("Address", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Address", "127.0.0.1:4391"));
            subdata.set("Password", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Password", ""));
            subdata.set("Encryption", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Encryption", "RSA/AES"));
            subdata.set("Whitelist", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawStringList("Whitelist", Collections.emptyList()));
            settings.set("SubData", subdata);

            rewritten.set("Settings", settings);


            YAMLSection hosts = new YAMLSection();
            for (String name : updated.getMap("Hosts", new YAMLSection()).getKeys()) {
                YAMLSection host = new YAMLSection();
                host.set("Enabled", updated.getMap("Hosts").getMap(name).getBoolean("Enabled", false));
                host.set("Display", updated.getMap("Hosts").getMap(name).getRawString("Display", ""));
                host.set("Driver", updated.getMap("Hosts").getMap(name).getRawString("Driver", "BUILT-IN"));
                host.set("Address", updated.getMap("Hosts").getMap(name).getRawString("Address", "127.0.0.1"));
                host.set("Port-Range", updated.getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559"));
                host.set("Directory", updated.getMap("Hosts").getMap(name).getRawString("Directory", (host.getRawString("Driver").equalsIgnoreCase("BUILT-IN"))?"./SubServers/Servers":"./Servers"));
                host.set("Git-Bash", updated.getMap("Hosts").getMap(name).getRawString("Git-Bash", "%ProgramFiles%\\Git"));
                host.set("Log-Creator", updated.getMap("Hosts").getMap(name).getBoolean("Log-Creator", true));
                if (updated.getMap("Hosts").getMap(name).contains("Extra")) host.set("Extra", updated.getMap("Hosts").getMap(name).getMap("Extra"));
                hosts.set(name, host);
            }
            rewritten.set("Hosts", hosts);

            config.set(rewritten);
            config.save();
        }
    }

    /**
     * Update SubServers' servers.yml
     *
     * @param file File to bring up-to-date
     */
    public static void updateServers(File file) throws IOException {
        YAMLConfig config = new YAMLConfig(file);
        YAMLSection updated = config.get().clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = updated.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!updated.contains("Settings") || !updated.getMap("Settings").contains("Version")) {
            YAMLSection servers = new YAMLSection();
            servers.set("Example", new YAMLSection());
            updated.set("Servers", servers);

            i++;
            System.out.println("SubServers > Created ./SubServers/servers.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                for (String name : updated.getMap("Servers", new YAMLSection()).getKeys()) {
                    if (updated.getMap("Servers").getMap(name).getBoolean("Auto-Restart", true))
                        updated.getMap("Servers").getMap(name).safeSet("Stop-Action", "RESTART");

                    if (updated.getMap("Servers").getMap(name).getRawString("Stop-Action", "NONE").equalsIgnoreCase("DELETE_SERVER"))
                        updated.getMap("Servers").getMap(name).set("Stop-Action", "RECYCLE_SERVER");
                }
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) System.out.println("SubServers > Updated ./SubServers/servers.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("Smart-Fallback", updated.getMap("Settings", new YAMLSection()).getBoolean("Smart-Fallback", true));
            settings.set("Run-On-Launch-Timeout", updated.getMap("Settings", new YAMLSection()).getInt("Run-On-Launch-Timeout", 0));

            rewritten.set("Settings", settings);


            YAMLSection servers = new YAMLSection();
            for (String name : updated.getMap("Servers", new YAMLSection()).getKeys()) {
                YAMLSection server = new YAMLSection();
                server.set("Enabled", updated.getMap("Servers").getMap(name).getBoolean("Enabled", false));
                server.set("Display", updated.getMap("Servers").getMap(name).getRawString("Display", ""));
                server.set("Host", updated.getMap("Servers").getMap(name).getRawString("Host", "~"));
                server.set("Group", updated.getMap("Servers").getMap(name).getRawStringList("Groups", Collections.emptyList()));
                server.set("Port", updated.getMap("Servers").getMap(name).getInt("Port", 25567));
                server.set("Motd", updated.getMap("Servers").getMap(name).getRawString("Motd", "Some SubServer"));
                server.set("Log", updated.getMap("Servers").getMap(name).getBoolean("Log", true));
                server.set("Directory", updated.getMap("Servers").getMap(name).getRawString("Directory", "." + File.separatorChar));
                server.set("Executable", updated.getMap("Servers").getMap(name).getRawString("Executable", "java -Xmx1024M -Djline.terminal=jline.UnsupportedTerminal -jar Spigot.jar"));
                server.set("Stop-Command", updated.getMap("Servers").getMap(name).getRawString("Stop-Command", "stop"));
                server.set("Stop-Action", updated.getMap("Servers").getMap(name).getRawString("Stop-Action", "NONE"));
                server.set("Run-On-Launch", updated.getMap("Servers").getMap(name).getBoolean("Run-On-Launch", false));
                server.set("Restricted", updated.getMap("Servers").getMap(name).getBoolean("Restricted", false));
                server.set("Incompatible", updated.getMap("Servers").getMap(name).getRawStringList("Incompatible", Collections.emptyList()));
                server.set("Hidden", updated.getMap("Servers").getMap(name).getBoolean("Hidden", false));
                if (updated.getMap("Servers").getMap(name).contains("Extra")) server.set("Extra", updated.getMap("Servers").getMap(name).getMap("Extra"));
                servers.set(name, server);
            }
            rewritten.set("Servers", servers);

            config.set(rewritten);
            config.save();
        }
    }

    /**
     * Update SubServers' lang.yml
     *
     * @param file File to bring up-to-date
     */
    public static void updateLang(File file) throws IOException {

    }
}
