package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Util;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * SubServers/BungeeCord Class
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
        final Container<Boolean> bungee = new Container<Boolean>(false);
        if (Util.isException(() -> {
            if (Class.forName("net.md_5.bungee.BungeeCord") != null) bungee.set(true);
        }) && !bungee.get()) {
            System.out.println("");
            System.out.println("*******************************************");
            System.out.println("*** Error: BungeeCord.jar Doesn't Exist ***");
            System.out.println("***                                     ***");
            System.out.println("***    Please download a build from:    ***");
            System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
            System.out.println("*******************************************");
            System.out.println("");
        } else {
            System.out.println("");
            System.out.println("*******************************************");
            System.out.println("***  Warning: this build is Unofficial  ***");
            System.out.println("***                                     ***");
            System.out.println("*** Please report all issues to ME1312, ***");
            System.out.println("***   NOT the Spigot Team. Thank You!   ***");
            System.out.println("*******************************************");

            Security.setProperty("networkaddress.cache.ttl", "30");
            Security.setProperty("networkaddress.cache.negative.ttl", "10");
            joptsimple.OptionParser parser = new joptsimple.OptionParser();
            parser.allowsUnrecognizedOptions();
            parser.accepts("v");
            parser.accepts("version");
            parser.accepts("noconsole");
            joptsimple.OptionSet options = parser.parse(args);
            if(options.has("version") || options.has("v")) {
                System.out.println("");
                System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + ",");
                System.out.println("Java " + System.getProperty("java.version") + ",");
                System.out.println("BungeeCord " + net.md_5.bungee.Bootstrap.class.getPackage().getImplementationVersion() + ",");
                System.out.println("SubServers v" + SubPlugin.class.getPackage().getImplementationVersion() + ((SubPlugin.class.getPackage().getSpecificationVersion().equals("0"))?"":" BETA "+SubPlugin.class.getPackage().getSpecificationVersion()));
                System.out.println("");
            } else {
                try {
                    if (net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion() != null) {
                        Date date = (new SimpleDateFormat("yyyyMMdd")).parse(net.md_5.bungee.BungeeCord.class.getPackage().getSpecificationVersion());
                        Calendar line = Calendar.getInstance();
                        line.add(3, -4);
                        if (date.before(line.getTime())) {
                            System.out.println("***   Warning: this build is outdated   ***");
                            System.out.println("***  Please download a new build from:  ***");
                            System.out.println("***  http://ci.md-5.net/job/BungeeCord  ***");
                            System.out.println("*** Errors may arise on older versions! ***");
                            System.out.println("*******************************************");
                        }
                    } else throw new Exception();
                } catch (Exception e) {
                    System.out.println("*** Problem checking BungeeCord Version ***");
                    System.out.println("***    This build could be outdated.    ***");
                    System.out.println("***                                     ***");
                    System.out.println("*** Errors may arise on older versions! ***");
                    System.out.println("*******************************************");
                }
                System.out.println("");

                SubPlugin plugin = new SubPlugin(System.out);
                net.md_5.bungee.api.ProxyServer.class.getMethod("setInstance", net.md_5.bungee.api.ProxyServer.class).invoke(null, plugin);
                plugin.getLogger().info("Enabled BungeeCord version " + plugin.getVersion());
                plugin.start();

                if (!options.has("noconsole")) {
                    String line;
                    while (plugin.isRunning && (line = plugin.getConsoleReader().readLine(">")) != null) {
                        if (!plugin.getPluginManager().dispatchCommand(net.md_5.bungee.command.ConsoleCommandSender.class.cast(net.md_5.bungee.command.ConsoleCommandSender.class.getMethod("getInstance").invoke(null)), line)) {
                            plugin.getConsole().sendMessage(net.md_5.bungee.api.ChatColor.RED + "Command not found");
                        }
                    }
                }
            }
        }
    }
}