package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Galaxi.GalaxiInfo;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * SubServers/BungeeCord Launch Class
 */
public final class Launch {

    /**
     * Launch SubServers/BungeeCord
     *
     * @param args Launch Arguments
     * @throws Exception
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.lang.Process.allowAmbiguousCommands", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        if (Util.getDespiteException(() -> Class.forName("net.md_5.bungee.BungeeCord") == null, true)) {
            System.out.println("");
            System.out.println("*******************************************");
            System.out.println("*** Error: BungeeCord.jar Doesn't Exist ***");
            System.out.println("***                                     ***");
            System.out.println("***    Please download a build from:    ***");
            System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
            System.out.println("*******************************************");
            System.out.println("");
            System.exit(1);
        } else {
            Security.setProperty("networkaddress.cache.ttl", "30");
            Security.setProperty("networkaddress.cache.negative.ttl", "10");
            final boolean patched = net.md_5.bungee.BungeeCord.class.getPackage().getImplementationTitle() != null && net.md_5.bungee.BungeeCord.class.getPackage().getImplementationTitle().equals("SubServers.Bungee");

            joptsimple.OptionParser parser = new joptsimple.OptionParser();
            parser.allowsUnrecognizedOptions();
            parser.accepts("v");
            parser.accepts("version");
            parser.accepts("noconsole");
            joptsimple.OptionSet options = parser.parse(args);
            if(options.has("version") || options.has("v")) {
                Version galaxi = GalaxiInfo.getVersion();
                Version galaxibuild = GalaxiInfo.getBuild();

                System.out.println("");
                System.out.println(Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((!Platform.getSystemVersion().equals(Platform.getSystemBuild()))?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ',');
                System.out.println("Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ',');
                if (galaxi != null) System.out.println("GalaxiEngine v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"") + ',');
                System.out.println("BungeeCord " + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ((patched)?" [Patched]":"") + ',');
                System.out.println("SubServers.Bungee v" + SubProxy.version.toExtendedString() + ((SubProxy.class.getPackage().getSpecificationTitle() != null)?" (" + SubProxy.class.getPackage().getSpecificationTitle() + ')':""));
                System.out.println("");
            } else {
                boolean gb = Util.getDespiteException(() -> Class.forName("net.md_5.bungee.util.GalaxiBungeeInfo").getMethod("get").getReturnType().equals(Class.forName("net.ME1312.Galaxi.Plugin.PluginInfo")), false);
                if (gb) {
                    Util.reflect(net.md_5.bungee.log.LoggingOutputStream.class.getMethod("setLogger", Logger.class, String.class), null,
                            Util.reflect(net.md_5.bungee.log.BungeeLogger.class.getMethod("get", String.class), null, "SubServers"), "net.ME1312.SubServers.Bungee.");
                } else {
                    System.out.println("");
                    System.out.println("*******************************************");
                    System.out.println("***  Warning: this build is unofficial  ***");
                    System.out.println("***                                     ***");
                    System.out.println("*** Please report all issues to ME1312, ***");
                    System.out.println("***   NOT the Spigot Team. Thank You!   ***");
                    System.out.println("*******************************************");
                    try {
                        if (net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion() != null) {
                            Date date = (new SimpleDateFormat("yyyyMMdd")).parse(net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion());
                            Calendar line = Calendar.getInstance();
                            line.add(3, -4);
                            if (date.before(line.getTime())) {
                                System.out.println("***   Warning: BungeeCord is outdated   ***");
                                System.out.println("***  Please download a new build from:  ***");
                                System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
                                System.out.println("*** Errors may arise on older versions! ***");
                                System.out.println("*******************************************");
                            }
                        } else throw new Exception();
                    } catch (Exception e) {
                        System.out.println("*** Problem checking BungeeCord version ***");
                        System.out.println("***    BungeeCord could be outdated.    ***");
                        System.out.println("***                                     ***");
                        System.out.println("*** Errors may arise on older versions! ***");
                        System.out.println("*******************************************");
                    }
                    System.out.println("");
                }

                SubProxy plugin = new SubProxy(System.out, patched);
                net.md_5.bungee.api.ProxyServer.class.getMethod("setInstance", net.md_5.bungee.api.ProxyServer.class).invoke(null, plugin);
                if (!gb) plugin.getLogger().info("Enabled " + plugin.getBungeeName() + " version " + plugin.getVersion());
                plugin.start();

                if (!options.has("noconsole")) {
                    if (!gb) try {
                        if (Util.getDespiteException(() -> Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands") != null, false)) { // Waterfall Setup
                            Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands").invoke(null);
                        } else if (Util.getDespiteException(() -> Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("start") != null, false)) {
                            Class console = Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole");
                            console.getMethod("start").invoke(console.getConstructor().newInstance());
                        } else {
                            plugin.canSudo = true;
                            String line;
                            while (plugin.isRunning && (line = plugin.getConsoleReader().readLine(">")) != null) {
                                if (plugin.sudo == null) {
                                    if (!plugin.getPluginManager().dispatchCommand(net.md_5.bungee.command.ConsoleCommandSender.class.cast(net.md_5.bungee.command.ConsoleCommandSender.class.getMethod("getInstance").invoke(null)), line)) {
                                        plugin.getConsole().sendMessage(net.md_5.bungee.api.ChatColor.RED + "Command not found");
                                    }
                                } else if (line.equalsIgnoreCase("exit")) {
                                    plugin.sudo = null;
                                    net.ME1312.SubServers.Bungee.Library.Compatibility.Logger.get("SubServers").info("Reverting to the BungeeCord Console");
                                } else {
                                    plugin.sudo.command(line);
                                }
                            }
                        }
                    } catch (NoSuchMethodError | NoSuchMethodException e) {
                        plugin.getLogger().warning("Standard BungeeCord console not found; Console commands may now be disabled.");
                    }
                }
            }
        }
    }
}