package net.ME1312.SubServers.Bungee.Library;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.SubAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            YAMLSection hosts = new YAMLSection();
            YAMLSection host = new YAMLSection();
            host.set("Enabled", true);
            host.set("Display", "Default");
            hosts.set("~", host);
            updated.set("Hosts", hosts);

            i++;
            Logger.get("SubServers").info("Created ./SubServers/config.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                if (existing.getMap("Settings", new YAMLSection()).contains("Log-Creator")) for (String name : existing.getMap("Hosts", new YAMLSection()).getKeys())
                    updated.getMap("Hosts").getMap(name).safeSet("Log-Creator", existing.getMap("Settings").getBoolean("Log-Creator"));

                if (existing.getMap("Settings", new YAMLSection()).contains("SubData") && !existing.getMap("Settings", new YAMLSection()).getMap("SubData").contains("Encryption"))
                    updated.getMap("Settings").getMap("SubData").set("Encryption", "NONE");

                if (existing.contains("Servers")) {
                    YAMLConfig sc = new YAMLConfig(new File(file.getParentFile(), "servers.yml"));
                    YAMLSection settings = new YAMLSection();
                    settings.set("Version", was.toString());
                    settings.set("Run-On-Launch-Timeout", (existing.getMap("Settings", new YAMLSection()).contains("Run-On-Launch-Timeout"))?existing.getMap("Settings").getInt("Run-On-Launch-Timeout"):0);
                    sc.get().safeSet("Settings", settings);

                    sc.get().safeSet("Servers", new YAMLSection());
                    sc.get().getMap("Servers").safeSetAll(existing.getMap("Servers"));
                    Logger.get("SubServers").info("Created ./SubServers/servers.yml (using existing data)");
                    sc.save();
                }

                existing = updated.clone();
                i++;
            } if (was.compareTo(new Version("19w35c")) <= 0) {
                if (existing.getMap("Settings", new YAMLSection()).contains("SubData")) {
                    LinkedList<String> whitelist = new LinkedList<>();
                    LinkedList<String> newWhitelist = new LinkedList<>();
                    whitelist.addAll(existing.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getStringList("Allowed-Connections", Collections.emptyList()));
                    whitelist.addAll(existing.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getStringList("Whitelist", Collections.emptyList()));

                    boolean warnPls = false;
                    for (String address : whitelist) {
                        Matcher regAddress = Pattern.compile("^(\\d{1,3}|%)\\.(\\d{1,3}|%)\\.(\\d{1,3}|%)\\.(\\d{1,3}|%)$").matcher(address);
                        if (regAddress.find()) {
                            StringBuilder newAddress = new StringBuilder();
                            int subnet = -1;
                            boolean warn = false;
                            for (int o = 1; o <= 4; o++) {
                                if (o > 1) newAddress.append('.');
                                if (subnet == -1) {
                                    if (!regAddress.group(o).equals("%")) {
                                        newAddress.append(regAddress.group(o));
                                    } else {
                                        subnet = 8 * (o - 1);
                                        newAddress.append('0');
                                    }
                                } else {
                                    if (!regAddress.group(o).equals("%")) warn = warnPls = true;
                                    newAddress.append('0');
                                }
                            }
                            if (subnet < 0) subnet = 32;
                            if (warn) Logger.get("SubServers").warning("Updating non-standard mask: " + address);
                            newAddress.append('/');
                            newAddress.append(subnet);
                            newWhitelist.add(newAddress.toString());
                        }
                    }
                    updated.getMap("Settings").getMap("SubData").set("Whitelist", newWhitelist);
                    if (warnPls) Logger.get("SubServers").warning("Non-standard masks have been updated. This may expose SubData to unintended networks!");
                }

                existing = updated.clone();
                i++;
            } if (was.compareTo(new Version("20w08d")) <= 0) {
                if (existing.contains("Hosts")) {
                    for (String name : existing.getMap("Hosts", new YAMLSection()).getKeys()) {
                        if (existing.getMap("Hosts").getMap(name).getRawString("Driver", "BUILT_IN").replace('-', '_').replace(' ', '_').equalsIgnoreCase("BUILT_IN"))
                            updated.getMap("Hosts").getMap(name).set("Driver", "VIRTUAL");
                    }
                }

                existing = updated.clone();
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
            //  existing = updated.clone();
            //  i++
            //}

            if (i > 0) Logger.get("SubServers").info("Updated ./SubServers/config.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            if (updated.getMap("Settings", new YAMLSection()).contains("RemotePlayer-Cache-Interval")) settings.set("RemotePlayer-Cache-Interval", updated.getMap("Settings").getRawString("RemotePlayer-Cache-Interval"));
            settings.set("Disabled-Overrides", updated.getMap("Settings", new YAMLSection()).getRawStringList("Disabled-Overrides", Collections.emptyList()));

            YAMLSection smart_fallback = new YAMLSection();
            smart_fallback.set("Enabled", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Enabled", true));
            smart_fallback.set("Fallback", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Fallback", true));
            smart_fallback.set("Reconnect", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("Reconnect", false));
            smart_fallback.set("DNS-Forward", updated.getMap("Settings", new YAMLSection()).getMap("Smart-Fallback", new YAMLSection()).getBoolean("DNS-Forward", false));
            settings.set("Smart-Fallback", smart_fallback);

            YAMLSection upnp = new YAMLSection();
            upnp.set("Forward-Proxy", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true));
            upnp.set("Forward-SubData", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-SubData", false));
            upnp.set("Forward-Servers", updated.getMap("Settings", new YAMLSection()).getMap("UPnP", new YAMLSection()).getBoolean("Forward-Servers", false));
            settings.set("UPnP", upnp);

            YAMLSection subdata = new YAMLSection();
            subdata.set("Address", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Address", "127.0.0.1:4391"));
            if (updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).contains("Password")) subdata.set("Password", updated.getMap("Settings").getMap("SubData").getRawString("Password"));
            subdata.set("Encryption", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawString("Encryption", "RSA/AES"));
            subdata.set("Whitelist", updated.getMap("Settings", new YAMLSection()).getMap("SubData", new YAMLSection()).getRawStringList("Whitelist", Collections.emptyList()));
            settings.set("SubData", subdata);

            rewritten.set("Settings", settings);


            YAMLSection hosts = new YAMLSection();
            for (String name : updated.getMap("Hosts", new YAMLSection()).getKeys()) {
                YAMLSection host = new YAMLSection();
                host.set("Enabled", updated.getMap("Hosts").getMap(name).getBoolean("Enabled", false));
                host.set("Display", updated.getMap("Hosts").getMap(name).getRawString("Display", ""));
                host.set("Driver", updated.getMap("Hosts").getMap(name).getRawString("Driver", "VIRTUAL"));
                host.set("Address", updated.getMap("Hosts").getMap(name).getRawString("Address", "127.0.0.1"));
                host.set("Port-Range", updated.getMap("Hosts").getMap(name).getRawString("Port-Range", "25500-25559"));
                host.set("Directory", updated.getMap("Hosts").getMap(name).getRawString("Directory", (host.getRawString("Driver").equalsIgnoreCase("VIRTUAL"))?"./SubServers/Servers":"./Servers"));
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
        YAMLSection existing = config.get().clone();
        YAMLSection updated = existing.clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = existing.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!existing.contains("Settings") || !existing.getMap("Settings").contains("Version")) {
            YAMLSection servers = new YAMLSection();
            servers.set("Example", new YAMLSection());
            updated.set("Servers", servers);

            i++;
            Logger.get("SubServers").info("Created ./SubServers/servers.yml");
        } else {
            if (was.compareTo(new Version("19w17a")) <= 0) {
                if (existing.contains("Servers")) {
                    for (String name : existing.getMap("Servers", new YAMLSection()).getKeys()) {
                        if (existing.getMap("Servers").getMap(name).getBoolean("Auto-Restart", true))
                            updated.getMap("Servers").getMap(name).safeSet("Stop-Action", "RESTART");

                        if (existing.getMap("Servers").getMap(name).getRawString("Stop-Action", "NONE").equalsIgnoreCase("DELETE_SERVER"))
                            updated.getMap("Servers").getMap(name).set("Stop-Action", "RECYCLE_SERVER");
                    }
                }

                existing = updated.clone();
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) Logger.get("SubServers").info("Updated ./SubServers/servers.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());
            settings.set("Run-On-Launch-Timeout", updated.getMap("Settings", new YAMLSection()).getInt("Run-On-Launch-Timeout", 0));

            rewritten.set("Settings", settings);


            YAMLSection servers = new YAMLSection();
            for (String name : updated.getMap("Servers", new YAMLSection()).getKeys()) {
                YAMLSection server = new YAMLSection();
                server.set("Enabled", updated.getMap("Servers").getMap(name).getBoolean("Enabled", false));
                server.set("Display", updated.getMap("Servers").getMap(name).getRawString("Display", ""));
                server.set("Host", updated.getMap("Servers").getMap(name).getRawString("Host", "~"));
                if (updated.getMap("Servers").getMap(name).contains("Template")) server.set("Template", updated.getMap("Servers").getMap(name).getRawString("Template"));
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
        YAMLSection existing = config.get().clone();
        YAMLSection updated = existing.clone();
        YAMLSection rewritten = new YAMLSection();

        Version was = existing.getMap("Settings", new ObjectMap<>()).getVersion("Version", new Version(0));
        Version now = SubAPI.getInstance().getWrapperBuild();

        int i = 0;
        if (now == null) now = UNSIGNED;
        if (!existing.contains("Settings") || !existing.getMap("Settings").contains("Version")) {

            i++;
            Logger.get("SubServers").info("Created ./SubServers/lang.yml");
        } else {
            if (was.compareTo(new Version("19w22b")) <= 0) {
                if (existing.contains("Lang")) {
                    updated.getMap("Lang").remove("Interface.Host-Admin.SubServers");
                    updated.getMap("Lang").remove("Interface.SubServer-Admin.Command");
                }

                existing = updated.clone();
                i++;
            } if (was.compareTo(new Version("20w08d")) <= 0) {
                if (existing.contains("Lang")) {
                    LinkedList<String> keys = new LinkedList<>(existing.getMap("Lang").getKeys());
                    for (String key : keys) if (key.startsWith("Command.")) {
                        updated.getMap("Lang").remove(key);
                    }
                }

                existing = updated.clone();
                i++;
            } if (was.compareTo(new Version("20w34c")) <= 0) {

              //existing = updated.clone();
                i++;
            }// if (was.compareTo(new Version("99w99a")) <= 0) {
            //  // do something
            //  i++
            //}

            if (i > 0) Logger.get("SubServers").info("Updated ./SubServers/lang.yml (" + i + " pass" + ((i != 1)?"es":"") + ")");
        }

        if (i > 0) {
            YAMLSection settings = new YAMLSection();
            settings.set("Version", ((now.compareTo(was) <= 0)?was:now).toString());

            rewritten.set("Settings", settings);

            LinkedHashMap<String, String> def = new LinkedHashMap<String, String>();
            def.put("Bungee.Feature.Smart-Fallback", "&6Returning from $str$: &r$msg$");
            def.put("Bungee.Feature.Smart-Fallback.Result", "&6You are now on $str$.");
            def.put("Bungee.Ping.Offline", "&6&l[&e&lWarning&6&l] &7Backend server(s) are not running");
            def.put("Bungee.Server.Current", "&6You are currently connected to $str$");
            def.put("Bungee.Server.Available", "&6You may connect to the following servers at this time:");
            def.put("Bungee.Server.List", "&6$str$");
            def.put("Bungee.Server.Hover", "$int$ player(s)\\n&oClick to connect to the server");
            def.put("Bungee.Server.Divider", "&6, ");
            def.put("Bungee.Server.Offline", "&cThe specified server is not currently running.");
            def.put("Bungee.Server.Invalid", "&cThe specified server does not exist.");
            def.put("Bungee.List.Format", "&a[$str$] &e($int$)&r: ");
            def.put("Bungee.List.List", "&f$str$");
            def.put("Bungee.List.Divider", "&f, ");
            def.put("Bungee.List.Total", "Total players online: $int$");
            def.put("Command.Generic.Player-Only", "&cSubServers &4&l\\u00BB&c The console cannot perform this command");
            def.put("Command.Generic.Console-Only", "&cSubServers &4&l\\u00BB&c This command is for console use only");
            def.put("Command.Generic.Usage", "&7SubServers &8&l\\u00BB&7 Usage: &f$str$");
            def.put("Command.Generic.Exception", "&cSubServers &4&l\\u00BB&c An unexpected exception has occurred while parsing this command");
            def.put("Command.Generic.Invalid-Subcommand", "&cSubServers &4&l\\u00BB&c Unknown sub-command: $str$");
            def.put("Command.Generic.Invalid-Permission", "&cSubServers &4&l\\u00BB&c You need &4&n$str$&c to use this command");
            def.put("Command.Generic.Invalid-Select-Permission", "&cSubServers &4&l\\u00BB&c You don't have permission to select &4$str$");
            def.put("Command.Generic.Unknown-Proxy", "&cSubServers &4&l\\u00BB&c There is no proxy with name &4$str$");
            def.put("Command.Generic.Unknown-Host", "&cSubServers &4&l\\u00BB&c There is no host with name &4$str$");
            def.put("Command.Generic.Unknown-Group", "&cSubServers &4&l\\u00BB&c There is no group with name &4$str$");
            def.put("Command.Generic.Unknown-Server", "&cSubServers &4&l\\u00BB&c There is no server with name &4$str$");
            def.put("Command.Generic.Unknown-SubServer", "&cSubServers &4&l\\u00BB&c There is no subserver with name &4$str$");
            def.put("Command.Generic.Unknown-Player", "&cSubServers &4&l\\u00BB&c There is no player with name &4$str$");
            def.put("Command.Generic.No-Servers-On-Host", "&7SubServers &8&l\\u00BB&7 There are no servers on host &f$str$");
            def.put("Command.Generic.No-SubServers-On-Host", "&7SubServers &8&l\\u00BB&7 There are no subservers on host &f$str$");
            def.put("Command.Generic.No-Servers-In-Group", "&7SubServers &8&l\\u00BB&7 There are no servers in group &f$str$");
            def.put("Command.Generic.No-SubServers-In-Group", "&7SubServers &8&l\\u00BB&7 There are no subservers in group &f$str$");
            def.put("Command.Generic.No-Servers-Selected", "&cSubServers &4&l\\u00BB&c No servers were selected");
            def.put("Command.Generic.No-SubServers-Selected", "&cSubServers &4&l\\u00BB&c No subservers were selected");
            def.put("Command.Help.Header", "&7SubServers &8&l\\u00BB&7 Command Help:");
            def.put("Command.Help.Help", "   &7Help:&f $str$");
            def.put("Command.Help.List", "   &7List:&f $str$");
            def.put("Command.Help.Version", "   &7Version:&f $str$");
            def.put("Command.Help.Info", "   &7Info:&f $str$");
            def.put("Command.Help.Host.Create", "   &7Create Server:&f $str$");
            def.put("Command.Help.SubServer.Start", "   &7Start Server:&f $str$");
            def.put("Command.Help.SubServer.Restart", "   &7Restart Server:&f $str$");
            def.put("Command.Help.SubServer.Stop", "   &7Stop Server:&f $str$");
            def.put("Command.Help.SubServer.Terminate", "   &7Terminate Server:&f $str$");
            def.put("Command.Help.SubServer.Command", "   &7Command Server:&f $str$");
            def.put("Command.Help.SubServer.Update", "   &7Update Server:&f $str$");
            def.put("Command.Version", "&7SubServers &8&l\\u00BB&7 These are the platforms and versions that are running &f$str$&7:");
            def.put("Command.Version.Outdated", "&7$name$ &f$str$ &7is available. You are $int$ version(s) behind.");
            def.put("Command.Version.Latest", "&7You are on the latest version.");
            def.put("Command.List.Group-Header", "&7SubServers &8&l\\u00BB&7 Group/Server List:");
            def.put("Command.List.Host-Header", "&7SubServers &8&l\\u00BB&7 Host/SubServer List:");
            def.put("Command.List.Server-Header", "&7SubServers &8&l\\u00BB&7 Server List:");
            def.put("Command.List.Proxy-Header", "&7SubServers &8&l\\u00BB&7 Proxy List:");
            def.put("Command.List.Header", "&7: ");
            def.put("Command.List.Divider", "&7, ");
            def.put("Command.List.Empty", "&7(none)");
            def.put("Command.Info", "&7SubServers &8&l\\u00BB&7 Info on $str$&7: &r");
            def.put("Command.Info.Unknown", "&cSubServers &4&l\\u00BB&c There is no object with that name");
            def.put("Command.Info.Unknown-Type", "&cSubServers &4&l\\u00BB&c There is no object type with that name");
            def.put("Command.Info.Unknown-Proxy", "&cSubServers &4&l\\u00BB&c There is no proxy with that name");
            def.put("Command.Info.Unknown-Host", "&cSubServers &4&l\\u00BB&c There is no host with that name");
            def.put("Command.Info.Unknown-Group", "&cSubServers &4&l\\u00BB&c There is no group with that name");
            def.put("Command.Info.Unknown-Server", "&cSubServers &4&l\\u00BB&c There is no server with that name");
            def.put("Command.Info.Unknown-Player", "&cSubServers &4&l\\u00BB&c There is no player with that name");
            def.put("Command.Info.Format", " -> &7$str$&7: &r");
            def.put("Command.Info.List", "  - ");
            def.put("Command.Start", "&aSubServers &2&l\\u00BB&a Started &2$int$&a subserver(s)");
            def.put("Command.Start.Disappeared", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c has disappeared");
            def.put("Command.Start.Host-Unavailable", "&cSubServers &4&l\\u00BB&c The host for &4$str$&c is not available");
            def.put("Command.Start.Host-Disabled", "&cSubServers &4&l\\u00BB&c The host for &4$str$&c is not enabled");
            def.put("Command.Start.Server-Unavailable", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c is not available");
            def.put("Command.Start.Server-Disabled", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c is not enabled");
            def.put("Command.Start.Server-Incompatible", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c cannot start while incompatible server(s) are running");
            def.put("Command.Start.Running", "&7SubServers &8&l\\u00BB&7 &f$int$&7 subserver(s) were already running");
            def.put("Command.Restart", "&aSubServers &2&l\\u00BB&a Restarting &2$int$&a subserver(s)");
            def.put("Command.Restart.Disappeared", "&cSubServers &4&l\\u00BB&c Could not restart server: Subserver &4$str$&c has disappeared");
            def.put("Command.Restart.Host-Unavailable", "&cSubServers &4&l\\u00BB&c Could not restart server: The host for &4$str$&c is no longer available");
            def.put("Command.Restart.Host-Disabled", "&cSubServers &4&l\\u00BB&c Could not restart server: The host for &4$str$&c is no longer enabled");
            def.put("Command.Restart.Server-Unavailable", "&cSubServers &4&l\\u00BB&c Could not restart server: Subserver &4$str$&c is no longer available");
            def.put("Command.Restart.Server-Disabled", "&cSubServers &4&l\\u00BB&c Could not restart server: Subserver &4$str$&c is no longer enabled");
            def.put("Command.Restart.Server-Incompatible", "&cSubServers &4&l\\u00BB&c Could not restart server: Subserver &4$str$&c cannot start while incompatible server(s) are running");
            def.put("Command.Stop", "&aSubServers &2&l\\u00BB&a Stopping &2$int$&a subserver(s)");
            def.put("Command.Stop.Disappeared", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c has disappeared");
            def.put("Command.Stop.Not-Running", "&7SubServers &8&l\\u00BB&7 &f$int$&7 subserver(s) were already offline");
            def.put("Command.Terminate", "&aSubServers &2&l\\u00BB&a Terminated &2$int$&a subserver(s)");
            def.put("Command.Terminate.Disappeared", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c has disappeared");
            def.put("Command.Terminate.Not-Running", "&7SubServers &8&l\\u00BB&7 &f$int$&7 subserver(s) were already offline");
            def.put("Command.Command", "&aSubServers &2&l\\u00BB&a Sent command to &2$int$&a subserver(s)");
            def.put("Command.Command.No-Command", "&cSubServers &4&l\\u00BB&c No command was entered");
            def.put("Command.Command.Not-Running", "&7SubServers &8&l\\u00BB&7 &f$int$&7 subserver(s) were offline");
            def.put("Command.Creator", "&aSubServers &2&l\\u00BB&a Creating subserver &2$str$&a");
            def.put("Command.Creator.Exists", "&cSubServers &4&l\\u00BB&c There is already a subserver with that name");
            def.put("Command.Creator.Unknown-Host", "&cSubServers &4&l\\u00BB&c There is no host with that name");
            def.put("Command.Creator.Host-Unavailable", "&cSubServers &4&l\\u00BB&c That host is not available");
            def.put("Command.Creator.Host-Disabled", "&cSubServers &4&l\\u00BB&c That host is not enabled");
            def.put("Command.Creator.Unknown-Template", "&cSubServers &4&l\\u00BB&c There is no template with that name");
            def.put("Command.Creator.Template-Disabled", "&cSubServers &4&l\\u00BB&c That template is not enabled");
            def.put("Command.Creator.Template-Invalid", "&cSubServers &4&l\\u00BB&c That template does not support subserver updating");
            def.put("Command.Creator.Version-Required", "&cSubServers &4&l\\u00BB&c That template requires a Minecraft version to be specified");
            def.put("Command.Creator.Invalid-Port", "&cSubServers &4&l\\u00BB&c Invalid port number");
            def.put("Command.Update", "&aSubServers &2&l\\u00BB&a Updating &2$int$&a subserver(s)");
            def.put("Command.Update.Disappeared", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c has disappeared");
            def.put("Command.Update.Host-Unavailable", "&cSubServers &4&l\\u00BB&c The host for &4$str$&c is not available");
            def.put("Command.Update.Host-Disabled", "&cSubServers &4&l\\u00BB&c The host for &4$str$&c is not enabled");
            def.put("Command.Update.Server-Unavailable", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c is not available");
            def.put("Command.Update.Running", "&cSubServers &4&l\\u00BB&c Cannot update &4$str$&c while it is still running");
            def.put("Command.Update.Unknown-Template", "&cSubServers &4&l\\u00BB&c We don't know which template created &4$str$");
            def.put("Command.Update.Template-Disabled", "&cSubServers &4&l\\u00BB&c The template that created &4$str$&c is not enabled");
            def.put("Command.Update.Template-Invalid", "&cSubServers &4&l\\u00BB&c The template that created &4$str$&c does not support subserver updating");
            def.put("Command.Update.Version-Required", "&cSubServers &4&l\\u00BB&c The template that created &4$str$&c requires a Minecraft version to be specified");
            def.put("Command.Teleport", "&aSubServers &2&l\\u00BB&a Teleporting &2$str$&a to server");
            def.put("Command.Teleport.Not-Running", "&cSubServers &4&l\\u00BB&c Subserver &4$str$&c is not running");
            def.put("Interface.Generic.Back", "&cBack");
            def.put("Interface.Generic.Back-Arrow", "&e&l<--");
            def.put("Interface.Generic.Next-Arrow", "&e&l-->");
            def.put("Interface.Generic.Undo", "&6Undo");
            def.put("Interface.Generic.Downloading", "&7SubServers &8&l\\u00BB&7 Downloading:&f $str$");
            def.put("Interface.Generic.Downloading.Title", "Downloading...");
            def.put("Interface.Generic.Downloading.Title-Color", "&b");
            def.put("Interface.Generic.Downloading.Title-Color-Alt", "&3");
            def.put("Interface.Generic.Downloading.Response", "&eWaiting for response");
            def.put("Interface.Generic.Invalid-Permission", "&4You need &n$str$");
            def.put("Interface.Proxy-Menu.Proxy-Player-Count", "&2$int$ Player(s) Online");
            def.put("Interface.Proxy-Menu.Proxy-Master", "&8Master Proxy");
            def.put("Interface.Proxy-Menu.Proxy-Disconnected", "&4Disconnected");
            def.put("Interface.Host-Menu.Title", "Host Menu");
            def.put("Interface.Host-Menu.Host-Unavailable", "&4Unavailable");
            def.put("Interface.Host-Menu.Host-Disabled", "&4Disabled");
            def.put("Interface.Host-Menu.Host-Server-Count", "&9$int$ Server(s)");
            def.put("Interface.Host-Menu.No-Hosts", "&c&oThere are No Hosts");
            def.put("Interface.Host-Menu.Group-Menu", "&6&lView Servers by Group");
            def.put("Interface.Host-Menu.Server-Menu", "&a&lView Servers");
            def.put("Interface.Host-Admin.Title", "Host/$str$");
            def.put("Interface.Host-Admin.Creator", "&eCreate a SubServer");
            def.put("Interface.Host-Admin.SubServers", "&bView SubServers");
            def.put("Interface.Host-Admin.Plugins", "&bPlugins...");
            def.put("Interface.Host-Creator.Title", "Host/$str$/Create");
            def.put("Interface.Host-Creator.Edit-Name", "Change Name");
            def.put("Interface.Host-Creator.Edit-Name.Title", "&eSubCreator\\n&6Enter a Name for this Server");
            def.put("Interface.Host-Creator.Edit-Name.Message", "&eSubCreator &6&l\\u00BB&e Enter a Name for this Server via Chat");
            def.put("Interface.Host-Creator.Edit-Name.Exists", "&cSubCreator &4&l\\u00BB&c There is already a SubServer with that name");
            def.put("Interface.Host-Creator.Edit-Name.Exists-Title", "&eSubCreator\\n&cThere is already a SubServer with that name");
            def.put("Interface.Host-Creator.Edit-Name.Invalid", "&cSubCreator &4&l\\u00BB&c Invalid Server Name");
            def.put("Interface.Host-Creator.Edit-Name.Invalid-Title", "&eSubCreator\\n&cInvalid Server Name");
            def.put("Interface.Host-Creator.Edit-Template", "Change Server Template");
            def.put("Interface.Host-Creator.Edit-Template.Title", "Host/$str$/Templates");
            def.put("Interface.Host-Creator.Edit-Template.No-Templates", "&c&oThere are No Templates");
            def.put("Interface.Host-Creator.Edit-Version", "Change Server Version");
            def.put("Interface.Host-Creator.Edit-Version.Title", "&eSubCreator\\n&6Enter a Server Version");
            def.put("Interface.Host-Creator.Edit-Version.Message", "&eSubCreator &6&l\\u00BB&e Enter a Server Version via Chat");
            def.put("Interface.Host-Creator.Edit-Port", "Change Server Port");
            def.put("Interface.Host-Creator.Edit-Port.Title", "&eSubCreator\\n&6Enter a Port Number");
            def.put("Interface.Host-Creator.Edit-Port.Message", "&eSubCreator &6&l\\u00BB&e Enter a Port Number via Chat");
            def.put("Interface.Host-Creator.Edit-Port.Invalid", "&cSubCreator &4&l\\u00BB&c Invalid Port Number");
            def.put("Interface.Host-Creator.Edit-Port.Invalid-Title", "&eSubCreator\\n&cInvalid Port Number");
            def.put("Interface.Host-Creator.Submit", "&eCreate SubServer");
            def.put("Interface.Host-Creator.Form-Incomplete", "&4Buttons above must be green");
            def.put("Interface.Host-Plugin.Title", "Host/$str$/Plugins");
            def.put("Interface.Host-Plugin.No-Plugins", "&c&oThere are No Plugins Available");
            def.put("Interface.Host-SubServer.Title", "Host/$str$/SubServers");
            def.put("Interface.Group-Menu.Title", "Group Menu");
            def.put("Interface.Group-Menu.Group-Server-Count", "&9$int$ Server(s)");
            def.put("Interface.Group-Menu.No-Groups", "&c&oThere are No Groups");
            def.put("Interface.Group-Menu.Server-Menu", "&a&lView All Servers");
            def.put("Interface.Group-SubServer.Title", "Group/$str$/Servers");
            def.put("Interface.Server-Menu.Title", "Server Menu");
            def.put("Interface.Server-Menu.Server-Player-Count", "&2$int$ Player(s) Online");
            def.put("Interface.Server-Menu.Server-External", "&7External Server");
            def.put("Interface.Server-Menu.SubServer-Temporary", "&9Temporary");
            def.put("Interface.Server-Menu.SubServer-Offline", "&6Offline");
            def.put("Interface.Server-Menu.SubServer-Incompatible", "&4Incompatible with $str$");
            def.put("Interface.Server-Menu.SubServer-Unavailable", "&4Unavailable");
            def.put("Interface.Server-Menu.SubServer-Disabled", "&4Disabled");
            def.put("Interface.Server-Menu.SubServer-Invalid", "&4Cannot be managed by SubServers");
            def.put("Interface.Server-Menu.No-Servers", "&c&oThere are No Servers");
            def.put("Interface.Server-Menu.Host-Menu", "&b&lView Hosts");
            def.put("Interface.SubServer-Admin.Title", "SubServer/$str$");
            def.put("Interface.SubServer-Admin.Start", "&aStart SubServer");
            def.put("Interface.SubServer-Admin.Start.Title", "&aStarting SubServer");
            def.put("Interface.SubServer-Admin.Stop", "&cStop SubServer");
            def.put("Interface.SubServer-Admin.Stop.Title", "&cStopping $str$");
            def.put("Interface.SubServer-Admin.Terminate", "&4Terminate SubServer");
            def.put("Interface.SubServer-Admin.Terminate.Title", "&cTerminating $str$");
            def.put("Interface.SubServer-Admin.Command", "&bSend a Command to the SubServer");
            def.put("Interface.SubServer-Admin.Command.Title", "&eSubServers\\n&6Enter a Command to send via Chat");
            def.put("Interface.SubServer-Admin.Command.Message", "&eSubServers &6&l\\u00BB&e Enter a Command to send via Chat");
            def.put("Interface.SubServer-Admin.Update", "&eUpdate SubServer");
            def.put("Interface.SubServer-Admin.Update.Title", "&eSubServers\\n&6Enter a Server Version to update to");
            def.put("Interface.SubServer-Admin.Update.Message", "&eSubServers &6&l\\u00BB&e Enter a Server Version to update to via Chat");
            def.put("Interface.SubServer-Admin.Plugins", "&bPlugins...");
            def.put("Interface.SubServer-Plugin.Title", "SubServer/$str$/Plugins");
            def.put("Interface.SubServer-Plugin.No-Plugins", "&c&oThere are No Plugins Available");

            YAMLSection lang = new YAMLSection();
            for (String key : def.keySet()) lang.set(key, updated.getMap("Lang", new YAMLSection()).getRawString(key, def.get(key)));
            rewritten.set("Lang", lang);

            config.set(rewritten);
            config.save();
        }
    }
}
