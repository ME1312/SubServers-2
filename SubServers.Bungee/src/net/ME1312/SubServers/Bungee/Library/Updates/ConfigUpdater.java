package net.ME1312.SubServers.Bungee.Library.Updates;

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
        YAMLConfig config = new YAMLConfig(file);
        YAMLSection updated = config.get().clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = updated.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!updated.contains("Settings") || !updated.getMap("Settings").contains("Version")) {

            i++;
            System.out.println("SubServers > Created ./SubServers/lang.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) System.out.println("SubServers > Updated ./SubServers/lang.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());

            rewritten.set("Settings", settings);


            YAMLSection lang = new YAMLSection();
            lang.set("Bungee.Feature.Smart-Fallback", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Feature.Smart-Fallback", "&6Returning from $str$: &r$msg$"));
            lang.set("Bungee.Feature.Smart-Fallback.Result", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Feature.Smart-Fallback.Result", "&6You are now on $str$."));
            lang.set("Bungee.Ping.Offline", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Ping.Offline", "&6&l[&e&lWarning&6&l] &7Backend server(s) are not running"));
            lang.set("Bungee.Server.Current", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Current", "&6You are currently connected to $str$"));
            lang.set("Bungee.Server.Available", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Available", "&6You may connect to the following servers at this tRime:"));
            lang.set("Bungee.Server.List", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.List", "&6$str$"));
            lang.set("Bungee.Server.Hover", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Hover", "$int$ player(s)\n&oClick to connect to the server"));
            lang.set("Bungee.Server.Divider", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Divider", "&6, "));
            lang.set("Bungee.Server.Offline", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Offline", "&cThe specified server is not currently running."));
            lang.set("Bungee.Server.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.Server.Invalid", "&cThe specified server does not exist."));
            lang.set("Bungee.List.Format", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.List.Format", "&a[$str$] &e($int$)&r: "));
            lang.set("Bungee.List.List", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.List.List", "&f$str$"));
            lang.set("Bungee.List.Divider", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.List.Divider", "&f, "));
            lang.set("Bungee.List.Total", updated.getMap("Lang", new YAMLSection()).getRawString("Bungee.List.Total", "Total players online: $int$"));
            lang.set("Command.Generic.Player-Only", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Player-Only", "&cSubServers &4&l\u00BB&c Console cannot run this command"));
            lang.set("Command.Generic.Console-Only", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Console-Only", "&cSubServers &4&l\u00BB&c This command is for console use only"));
            lang.set("Command.Generic.Usage", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Usage", "&7SubServers &8&l\u00BB&7 Usage: &f$str$"));
            lang.set("Command.Generic.Exception", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Exception", "&cSubServers &4&l\u00BB&c An unexpected exception has occurred while parsing this command"));
            lang.set("Command.Generic.Invalid-Subcommand", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Invalid-Subcommand", "&cSubServers &4&l\u00BB&c Unknown sub-command: $str$"));
            lang.set("Command.Generic.Invalid-Permission", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Invalid-Permission", "&cSubServers &4&l\u00BB&c You need &4&n$str$&c to use this command"));
            lang.set("Command.Generic.Unknown-Proxy", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Unknown-Proxy", "&cSubServers &4&l\u00BB&c There is no proxy with that name"));
            lang.set("Command.Generic.Unknown-Host", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Unknown-Host", "&cSubServers &4&l\u00BB&c There is no host with that name"));
            lang.set("Command.Generic.Unknown-Group", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Unknown-Group", "&cSubServers &4&l\u00BB&c There is no group with that name"));
            lang.set("Command.Generic.Unknown-Server", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Unknown-Server", "&cSubServers &4&l\u00BB&c There is no server with that name"));
            lang.set("Command.Generic.Unknown-SubServer", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Generic.Unknown-SubServer", "&cSubServers &4&l\u00BB&c There is no subserver with that name"));
            lang.set("Command.Help.Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.Header", "&7SubServers &8&l\u00BB&7 Command Help:"));
            lang.set("Command.Help.Help", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.Help", "   &7Help:&f $str$"));
            lang.set("Command.Help.List", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.List", "   &7List:&f $str$"));
            lang.set("Command.Help.Version", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.Version", "   &7Version:&f $str$"));
            lang.set("Command.Help.Info", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.Info", "   &7Info:&f $str$"));
            lang.set("Command.Help.Host.Create", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.Host.Create", "   &7Create Server:&f $str$"));
            lang.set("Command.Help.SubServer.Start", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.SubServer.Start", "   &7Start Server:&f $str$"));
            lang.set("Command.Help.SubServer.Restart", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.SubServer.Restart", "   &7Restart Server:&f $str$"));
            lang.set("Command.Help.SubServer.Stop", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.SubServer.Stop", "   &7Stop Server:&f $str$"));
            lang.set("Command.Help.SubServer.Terminate", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.SubServer.Terminate", "   &7Terminate Server:&f $str$"));
            lang.set("Command.Help.SubServer.Command", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Help.SubServer.Command", "   &7Command Server:&f $str$"));
            lang.set("Command.Version", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Version", "&7SubServers &8&l\u00BB&7 These are the platforms and versions that are running &f$str$&7:"));
            lang.set("Command.Version.Outdated", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Version.Outdated", "&7$name$ &f$str$ &7is available. You are $int$ version(s) behind."));
            lang.set("Command.Version.Latest", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Version.Latest", "&7You are on the latest version."));
            lang.set("Command.List.Group-Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Group-Header", "&7SubServers &8&l\u00BB&7 Group/Server List:"));
            lang.set("Command.List.Host-Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Host-Header", "&7SubServers &8&l\u00BB&7 Host/SubServer List:"));
            lang.set("Command.List.Server-Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Server-Header", "&7SubServers &8&l\u00BB&7 Server List:"));
            lang.set("Command.List.Proxy-Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Proxy-Header", "&7SubServers &8&l\u00BB&7 Proxy List:"));
            lang.set("Command.List.Header", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Header", "&7: "));
            lang.set("Command.List.Divider", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Divider", "&7, "));
            lang.set("Command.List.Empty", updated.getMap("Lang", new YAMLSection()).getRawString("Command.List.Empty", "&7(none)"));
            lang.set("Command.Info", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info", "&7SubServers &8&l\u00BB&7 Info on $str$&7: &r"));
            lang.set("Command.Info.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown", "&cSubServers &4&l\u00BB&c There is no object with that name"));
            lang.set("Command.Info.Unknown-Type", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown-Type", "&cSubServers &4&l\u00BB&c There is no object type with that name"));
            lang.set("Command.Info.Unknown-Proxy", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown-Proxy", "&cSubServers &4&l\u00BB&c There is no proxy with that name"));
            lang.set("Command.Info.Unknown-Host", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown-Host", "&cSubServers &4&l\u00BB&c There is no host with that name"));
            lang.set("Command.Info.Unknown-Group", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown-Group", "&cSubServers &4&l\u00BB&c There is no group with that name"));
            lang.set("Command.Info.Unknown-Server", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Unknown-Server", "&cSubServers &4&l\u00BB&c There is no server with that name"));
            lang.set("Command.Info.Format", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.Format", " -> &7$str$&7: &r"));
            lang.set("Command.Info.List", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Info.List", "  - "));
            lang.set("Command.Start", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start", "&aSubServers &2&l\u00BB&a Starting SubServer"));
            lang.set("Command.Start.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Unknown", "&cSubServers &4&l\u00BB&c There is no Server with that name"));
            lang.set("Command.Start.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Invalid", "&cSubServers &4&l\u00BB&c That Server is not a SubServer"));
            lang.set("Command.Start.Host-Unavailable", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Host-Unavailable", "&cSubServers &4&l\u00BB&c That SubServer\u0027s Host is not available"));
            lang.set("Command.Start.Host-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Host-Disabled", "&cSubServers &4&l\u00BB&c That SubServer\u0027s Host is not enabled"));
            lang.set("Command.Start.Server-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Server-Disabled", "&cSubServers &4&l\u00BB&c That SubServer is not enabled"));
            lang.set("Command.Start.Server-Incompatible", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Server-Incompatible", "&cSubServers &4&l\u00BB&c That SubServer cannot start while these server(s) are running: &4$str$"));
            lang.set("Command.Start.Running", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Start.Running", "&cSubServers &4&l\u00BB&c That SubServer is already running"));
            lang.set("Command.Restart", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart", "&aSubServers &2&l\u00BB&a Stopping SubServer"));
            lang.set("Command.Restart.Finish", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Finish", "&aSubServers &2&l\u00BB&a Starting SubServer"));
            lang.set("Command.Restart.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Unknown", "&cSubServers &4&l\u00BB&c There is no Server with that name"));
            lang.set("Command.Restart.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Invalid", "&cSubServers &4&l\u00BB&c That Server is not a SubServer"));
            lang.set("Command.Restart.Disappeared", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Disappeared", "&cSubServers &4&l\u00BB&c Could not restart server: That SubServer has disappeared"));
            lang.set("Command.Restart.Host-Unavailable", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Host-Unavailable", "&cSubServers &4&l\u00BB&c Could not restart server: That SubServer\u0027s Host is no longer available"));
            lang.set("Command.Restart.Host-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Host-Disabled", "&cSubServers &4&l\u00BB&c Could not restart server: That SubServer\u0027s Host is no longer enabled"));
            lang.set("Command.Restart.Server-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Server-Disabled", "&cSubServers &4&l\u00BB&c Could not restart server: That SubServer is no longer enabled"));
            lang.set("Command.Restart.Server-Incompatible", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Restart.Server-Incompatible", "&cSubServers &4&l\u00BB&c Could not restart server: That SubServer cannot start while these server(s) are running: &4$str$"));
            lang.set("Command.Stop", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Stop", "&aSubServers &2&l\u00BB&a Stopping SubServer"));
            lang.set("Command.Stop.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Stop.Unknown", "&cSubServers &4&l\u00BB&c There is no Server with that name"));
            lang.set("Command.Stop.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Stop.Invalid", "&cSubServers &4&l\u00BB&c That Server is not a SubServer"));
            lang.set("Command.Stop.Not-Running", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Stop.Not-Running", "&cSubServers &4&l\u00BB&c That SubServer is not running"));
            lang.set("Command.Terminate", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Terminate", "&aSubServers &2&l\u00BB&a Stopping SubServer"));
            lang.set("Command.Terminate.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Terminate.Unknown", "&cSubServers &4&l\u00BB&c There is no Server with that name"));
            lang.set("Command.Terminate.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Terminate.Invalid", "&cSubServers &4&l\u00BB&c That Server is not a SubServer"));
            lang.set("Command.Terminate.Not-Running", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Terminate.Not-Running", "&cSubServers &4&l\u00BB&c That SubServer is not running"));
            lang.set("Command.Command", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Command", "&aSubServers &2&l\u00BB&a Sending command to SubServer"));
            lang.set("Command.Command.Unknown", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Command.Unknown", "&cSubServers &4&l\u00BB&c There is no Server with that name"));
            lang.set("Command.Command.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Command.Invalid", "&cSubServers &4&l\u00BB&c That Server is not a SubServer"));
            lang.set("Command.Command.Not-Running", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Command.Not-Running", "&cSubServers &4&l\u00BB&c That SubServer is not running"));
            lang.set("Command.Creator", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator", "&aSubServers &2&l\u00BB&a Creating SubServer"));
            lang.set("Command.Creator.Exists", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Exists", "&cSubServers &4&l\u00BB&c There is already a SubServer with that name"));
            lang.set("Command.Creator.Unknown-Host", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Unknown-Host", "&cSubServers &4&l\u00BB&c There is no Host with that name"));
            lang.set("Command.Creator.Host-Unavailable", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Host-Unavailable", "&cSubServers &4&l\u00BB&c That Host is not available"));
            lang.set("Command.Creator.Host-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Host-Disabled", "&cSubServers &4&l\u00BB&c That Host is not enabled"));
            lang.set("Command.Creator.Unknown-Template", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Unknown-Template", "&cSubServers &4&l\u00BB&c There is no Template with that name"));
            lang.set("Command.Creator.Template-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Template-Disabled", "&cSubServers &4&l\u00BB&c That Template is not enabled"));
            lang.set("Command.Creator.Version-Required", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Version-Required", "&cSubServers &4&l\u00BB&c That Template requires a Minecraft Version to be specified"));
            lang.set("Command.Creator.Invalid-Port", updated.getMap("Lang", new YAMLSection()).getRawString("Command.Creator.Invalid-Port", "&cSubServers &4&l\u00BB&c Invalid Port Number"));
            lang.set("Interface.Generic.Back", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Back", "&cBack"));
            lang.set("Interface.Generic.Back-Arrow", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Back-Arrow", "&e&l<--"));
            lang.set("Interface.Generic.Next-Arrow", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Next-Arrow", "&e&l-->"));
            lang.set("Interface.Generic.Undo", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Undo", "&6Undo"));
            lang.set("Interface.Generic.Downloading", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Downloading", "&7SubServers &8&l\u00BB&7 Downloading:&f $str$"));
            lang.set("Interface.Generic.Downloading.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Downloading.Title", "Downloading..."));
            lang.set("Interface.Generic.Downloading.Title-Color", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Downloading.Title-Color", "&b"));
            lang.set("Interface.Generic.Downloading.Title-Color-Alt", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Downloading.Title-Color-Alt", "&3"));
            lang.set("Interface.Generic.Downloading.Response", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Downloading.Response", "&eWaiting for response"));
            lang.set("Interface.Generic.Invalid-Permission", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Generic.Invalid-Permission", "&4You need &n$str$"));
            lang.set("Interface.Proxy-Menu.Proxy-Player-Count", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Proxy-Menu.Proxy-Player-Count", "&2$int$ Player(s) Online"));
            lang.set("Interface.Proxy-Menu.Proxy-Master", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Proxy-Menu.Proxy-Master", "&8Master Proxy"));
            lang.set("Interface.Proxy-Menu.Proxy-SubData", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Proxy-Menu.Proxy-SubData", "&9SubData Only"));
            lang.set("Interface.Proxy-Menu.Proxy-Redis", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Proxy-Menu.Proxy-Redis", "&7Redis Only"));
            lang.set("Interface.Proxy-Menu.Proxy-Disconnected", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Proxy-Menu.Proxy-Disconnected", "&4Disconnected"));
            lang.set("Interface.Host-Menu.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Title", "Host Menu"));
            lang.set("Interface.Host-Menu.Host-Unavailable", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Host-Unavailable", "&4Unavailable"));
            lang.set("Interface.Host-Menu.Host-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Host-Disabled", "&4Disabled"));
            lang.set("Interface.Host-Menu.Host-Server-Count", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Host-Server-Count", "&9$int$ Server(s)"));
            lang.set("Interface.Host-Menu.No-Hosts", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.No-Hosts", "&c&oThere are No Hosts"));
            lang.set("Interface.Host-Menu.Group-Menu", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Group-Menu", "&6&lView Servers by Group"));
            lang.set("Interface.Host-Menu.Server-Menu", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Menu.Server-Menu", "&a&lView Servers"));
            lang.set("Interface.Host-Admin.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Admin.Title", "Host/$str$"));
            lang.set("Interface.Host-Admin.Creator", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Admin.Creator", "&eCreate a SubServer"));
            lang.set("Interface.Host-Admin.SubServers", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Admin.SubServers", "&aView SubServers"));
            lang.set("Interface.Host-Admin.Plugins", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Admin.Plugins", "&bPlugins..."));
            lang.set("Interface.Host-Creator.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Title", "Host/$str$/Create"));
            lang.set("Interface.Host-Creator.Edit-Name", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name", "Change Name"));
            lang.set("Interface.Host-Creator.Edit-Name.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Title", "&eSubCreator\n&6Enter a Name for this Server"));
            lang.set("Interface.Host-Creator.Edit-Name.Message", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Message", "&eSubCreator &6&l\u00BB&e Enter a Name for this Server via Chat"));
            lang.set("Interface.Host-Creator.Edit-Name.Exists", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Exists", "&cSubCreator &4&l\u00BB&c There is already a SubServer with that name"));
            lang.set("Interface.Host-Creator.Edit-Name.Exists-Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Exists-Title", "&eSubCreator\n&cThere is already a SubServer with that name"));
            lang.set("Interface.Host-Creator.Edit-Name.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Invalid", "&cSubCreator &4&l\u00BB&c Invalid Server Name"));
            lang.set("Interface.Host-Creator.Edit-Name.Invalid-Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Name.Invalid-Title", "&eSubCreator\n&cInvalid Server Name"));
            lang.set("Interface.Host-Creator.Edit-Template", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Template", "Change Server Template"));
            lang.set("Interface.Host-Creator.Edit-Template.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Template.Title", "Host/$str$/Templates"));
            lang.set("Interface.Host-Creator.Edit-Template.No-Templates", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Template.No-Templates", "&c&oThere are No Templates"));
            lang.set("Interface.Host-Creator.Edit-Version", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Version", "Change Server Version"));
            lang.set("Interface.Host-Creator.Edit-Version.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Version.Title", "&eSubCreator\n&6Enter a Server Version"));
            lang.set("Interface.Host-Creator.Edit-Version.Message", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Version.Message", "&eSubCreator &6&l\u00BB&e Enter a Server Version via Chat"));
            lang.set("Interface.Host-Creator.Edit-Port", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Port", "Change Server Port"));
            lang.set("Interface.Host-Creator.Edit-Port.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Port.Title", "&eSubCreator\n&6Enter a Port Number"));
            lang.set("Interface.Host-Creator.Edit-Port.Message", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Port.Message", "&eSubCreator &6&l\u00BB&e Enter a Port Number via Chat"));
            lang.set("Interface.Host-Creator.Edit-Port.Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Port.Invalid", "&cSubCreator &4&l\u00BB&c Invalid Port Number"));
            lang.set("Interface.Host-Creator.Edit-Port.Invalid-Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Edit-Port.Invalid-Title", "&eSubCreator\n&cInvalid Port Number"));
            lang.set("Interface.Host-Creator.Submit", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Submit", "&eCreate SubServer"));
            lang.set("Interface.Host-Creator.Form-Incomplete", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Creator.Form-Incomplete", "&4Buttons above must be green"));
            lang.set("Interface.Host-Plugin.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Plugin.Title", "Host/$str$/Plugins"));
            lang.set("Interface.Host-Plugin.No-Plugins", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-Plugin.No-Plugins", "&c&oThere are No Plugins Available"));
            lang.set("Interface.Host-SubServer.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Host-SubServer.Title", "Host/$str$/SubServers"));
            lang.set("Interface.Group-Menu.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Group-Menu.Title", "Group Menu"));
            lang.set("Interface.Group-Menu.Group-Server-Count", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Group-Menu.Group-Server-Count", "&9$int$ Server(s)"));
            lang.set("Interface.Group-Menu.No-Groups", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Group-Menu.No-Groups", "&c&oThere are No Groups"));
            lang.set("Interface.Group-Menu.Server-Menu", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Group-Menu.Server-Menu", "&a&lView All Servers"));
            lang.set("Interface.Group-SubServer.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Group-SubServer.Title", "Group/$str$/Servers"));
            lang.set("Interface.Server-Menu.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.Title", "Server Menu"));
            lang.set("Interface.Server-Menu.Server-Player-Count", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.Server-Player-Count", "&2$int$ Player(s) Online"));
            lang.set("Interface.Server-Menu.Server-External", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.Server-External", "&7External Server"));
            lang.set("Interface.Server-Menu.SubServer-Temporary", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.SubServer-Temporary", "&9Temporary"));
            lang.set("Interface.Server-Menu.SubServer-Offline", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.SubServer-Offline", "&6Offline"));
            lang.set("Interface.Server-Menu.SubServer-Incompatible", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.SubServer-Incompatible", "&4Incompatible with $str$"));
            lang.set("Interface.Server-Menu.SubServer-Disabled", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.SubServer-Disabled", "&4Disabled"));
            lang.set("Interface.Server-Menu.SubServer-Invalid", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.SubServer-Invalid", "&4Cannot be managed by SubServers"));
            lang.set("Interface.Server-Menu.No-Servers", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.No-Servers", "&c&oThere are No Servers"));
            lang.set("Interface.Server-Menu.Host-Menu", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.Server-Menu.Host-Menu", "&b&lView Hosts"));
            lang.set("Interface.SubServer-Admin.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Title", "SubServer/$str$"));
            lang.set("Interface.SubServer-Admin.Start", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Start", "&aStart SubServer"));
            lang.set("Interface.SubServer-Admin.Start.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Start.Title", "&aStarting SubServer"));
            lang.set("Interface.SubServer-Admin.Stop", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Stop", "&cStop SubServer"));
            lang.set("Interface.SubServer-Admin.Stop.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Stop.Title", "&cStopping $str$"));
            lang.set("Interface.SubServer-Admin.Terminate", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Terminate", "&4Terminate SubServer"));
            lang.set("Interface.SubServer-Admin.Terminate.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Terminate.Title", "&cTerminating $str$"));
            lang.set("Interface.SubServer-Admin.Command", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Command", "&eSend a Command to the SubServer"));
            lang.set("Interface.SubServer-Admin.Command.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Command.Title", "&eSubServers\n&6Enter a Command to send via Chat"));
            lang.set("Interface.SubServer-Admin.Command.Message", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Command.Message", "&eSubServers &6&l\u00BB&e Enter a Command to send via Chat"));
            lang.set("Interface.SubServer-Admin.Plugins", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Admin.Plugins", "&bPlugins..."));
            lang.set("Interface.SubServer-Plugin.Title", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Plugin.Title", "SubServer/$str$/Plugins"));
            lang.set("Interface.SubServer-Plugin.No-Plugins", updated.getMap("Lang", new YAMLSection()).getRawString("Interface.SubServer-Plugin.No-Plugins", "&c&oThere are No Plugins Available"));

            rewritten.set("Lang", lang);

            config.set(rewritten);
            config.save();
        }
    }
}
