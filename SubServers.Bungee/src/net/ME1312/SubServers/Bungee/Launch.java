package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Library.Version.VersionType;

import java.lang.reflect.Field;
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
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        Container<Boolean> bungee = new Container<Boolean>(false);
        if (Util.isException(() -> bungee.set(Class.forName("net.md_5.bungee.BungeeCord") != null)) && !bungee.get()) {
            System.out.println("");
            System.out.println("*******************************************");
            System.out.println("*** Error: BungeeCord.jar Doesn't Exist ***");
            System.out.println("***                                     ***");
            System.out.println("***    Please download a build from:    ***");
            System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
            System.out.println("*******************************************");
            System.out.println("");
            System.exit(1);
        } else if (System.getProperty("RM.subservers", "true").equalsIgnoreCase("true")) {
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
                boolean build = false;
                try {
                    Field f = Version.class.getDeclaredField("type");
                    f.setAccessible(true);
                    build = f.get(SubPlugin.version) != VersionType.SNAPSHOT && SubPlugin.class.getPackage().getSpecificationTitle() != null;
                    f.setAccessible(false);
                } catch (Exception e) {}

                System.out.println("");
                System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + ',');
                System.out.println("Java " + System.getProperty("java.version") + ",");
                System.out.println("BungeeCord" + ((patched)?" [Patched] ":" ") + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ',');
                System.out.println("SubServers.Bungee v" + SubPlugin.version.toExtendedString() + ((build)?" (" + SubPlugin.class.getPackage().getSpecificationTitle() + ')':""));
                System.out.println("");
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

                SubPlugin plugin = new SubPlugin(System.out, patched);
                net.md_5.bungee.api.ProxyServer.class.getMethod("setInstance", net.md_5.bungee.api.ProxyServer.class).invoke(null, plugin);
                plugin.getLogger().info("Enabled " + plugin.getBungeeName() + " version " + plugin.getVersion());
                plugin.start();

                if (!options.has("noconsole")) {
                    try {
                        Container<Boolean> proprietary = new Container<Boolean>(false);
                        if (!Util.isException(() -> proprietary.set(Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole") != null)) && proprietary.get()) {
                            Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands").invoke(null);
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
                                    System.out.println("SubServers > Reverting to the BungeeCord Console");
                                } else {
                                    plugin.sudo.command(line);
                                }
                            }
                        }
                    } catch (NoSuchMethodError | NoSuchMethodException e) {
                        plugin.canSudo = false;
                        plugin.getLogger().warning("Standard BungeeCord console not found; Console commands may now be disabled.");
                    }
                }
            }
        } else {
            System.out.println("");
            System.out.println("*******************************************");
            System.out.println("*** SubServers code has been disallowed ***");
            System.out.println("*** to work on this machine. Check with ***");
            System.out.println("*** your provider for more information. ***");
            System.out.println("*** Attempting re-launch as BungeeCord. ***");
            System.out.println("*******************************************");
            System.out.println("");
            net.md_5.bungee.BungeeCordLauncher.class.getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }
}