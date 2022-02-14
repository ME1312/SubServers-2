package net.ME1312.SubServers.Sync;

import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Try;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
        System.setProperty("jdk.util.jar.enableMultiRelease", "force");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        if (Try.all.get(() -> Class.forName("net.md_5.bungee.BungeeCord") == null, true)) {
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
            final boolean patched = net.md_5.bungee.BungeeCord.class.getPackage().getImplementationTitle() != null && net.md_5.bungee.BungeeCord.class.getPackage().getImplementationTitle().equals("SubServers.Sync");

            joptsimple.OptionParser parser = new joptsimple.OptionParser();
            parser.allowsUnrecognizedOptions();
            parser.accepts("v");
            parser.accepts("version");
            parser.accepts("noconsole");
            joptsimple.OptionSet options = parser.parse(args);
            if (options.has("version") || options.has("v")) {
                System.out.println("");
                System.out.println(Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ',');
                System.out.println("Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ',');
                System.out.println("BungeeCord " + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ((patched)?" [Patched]":"") + ',');
                System.out.println("SubServers.Sync v" + ExProxy.version.toExtendedString() + ((ExProxy.class.getPackage().getSpecificationTitle() != null)?" (" + ExProxy.class.getPackage().getSpecificationTitle() + ')':""));
                System.out.println("");
            } else {
                System.out.println("");
                System.out.println("*******************************************");
                System.out.println("***  Warning: this build is Unofficial  ***");
                System.out.println("***                                     ***");
                System.out.println("*** Please report all issues to ME1312, ***");
                System.out.println("***   NOT the Spigot Team. Thank You!   ***");
                System.out.println("*******************************************");
                try {
                    if (net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion() != null) {
                        Date date = (new SimpleDateFormat("yyyyMMdd")).parse(net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion());
                        Calendar line = Calendar.getInstance();
                        line.add(Calendar.WEEK_OF_YEAR, -8);
                        if (date.before(line.getTime())) {
                            System.out.println("***   Warning: BungeeCord is outdated   ***");
                            System.out.println("***  Please download a new build from:  ***");
                            System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
                            System.out.println("*** Errors may arise on older versions! ***");
                            System.out.println("*******************************************");
                        }
                    } else throw new Exception();
                } catch (Exception e) {
                    System.out.println("*** Problem checking BungeeCord Version ***");
                    System.out.println("***    BungeeCord could be outdated.    ***");
                    System.out.println("***                                     ***");
                    System.out.println("*** Errors may arise on older versions! ***");
                    System.out.println("*******************************************");
                }
                System.out.println("");

                ExProxy plugin = new ExProxy(System.out, patched);
                net.md_5.bungee.api.ProxyServer.class.getMethod("setInstance", net.md_5.bungee.api.ProxyServer.class).invoke(null, plugin);
                plugin.getLogger().info("Enabled " + plugin.getBungeeName() + " version " + plugin.getVersion());
                plugin.start();

                if (!options.has("noconsole")) {
                    try {
                        if (Try.all.get(() -> Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands") != null, false)) { // Waterfall Setup
                            Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands").invoke(null);
                        } else if (Try.all.get(() -> Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("start") != null, false)) {
                            Class console = Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole");
                            console.getMethod("start").invoke(console.getConstructor().newInstance());
                        } else {
                            String line;
                            while (plugin.isRunning && (line = plugin.getConsoleReader().readLine(">")) != null) {
                                if (!plugin.getPluginManager().dispatchCommand(net.md_5.bungee.command.ConsoleCommandSender.class.cast(net.md_5.bungee.command.ConsoleCommandSender.class.getMethod("getInstance").invoke(null)), line)) {
                                    plugin.getConsole().sendMessage(net.md_5.bungee.api.ChatColor.RED + "Command not found");
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