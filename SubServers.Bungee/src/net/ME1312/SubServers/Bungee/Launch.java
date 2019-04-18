package net.ME1312.SubServers.Bungee;

import net.ME1312.Galaxi.Library.Util;

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
                String osarch;
                if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                    String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                    String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

                    osarch = arch != null && arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64")?"x64":"x86";
                } else if (System.getProperty("os.arch").endsWith("86")) {
                    osarch = "x86";
                } else if (System.getProperty("os.arch").endsWith("64")) {
                    osarch = "x64";
                } else {
                    osarch = System.getProperty("os.arch");
                }

                String javaarch = null;
                switch (System.getProperty("sun.arch.data.model")) {
                    case "32":
                        javaarch = "x86";
                        break;
                    case "64":
                        javaarch = "x64";
                        break;
                    default:
                        if (!System.getProperty("sun.arch.data.model").equalsIgnoreCase("unknown"))
                            javaarch = System.getProperty("sun.arch.data.model");
                }

                System.out.println("");
                System.out.println(System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                System.out.println("Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                System.out.println("BungeeCord" + ((patched)?" [Patched] ":" ") + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ',');
                System.out.println("SubServers.Bungee v" + SubPlugin.version.toExtendedString() + ((SubPlugin.class.getPackage().getSpecificationTitle() != null)?" (" + SubPlugin.class.getPackage().getSpecificationTitle() + ')':""));
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
                        if (Util.getDespiteException(() -> Class.forName("io.github.waterfallmc.waterfall.console.WaterfallConsole").getMethod("readCommands") != null, false)) {
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
                                    System.out.println("SubServers > Reverting to the BungeeCord Console");
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