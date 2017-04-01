package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Command;
import net.ME1312.SubServers.Host.API.Event.CommandPreProcessEvent;
import net.ME1312.SubServers.Host.API.Event.SubDisableEvent;
import net.ME1312.SubServers.Host.API.Event.SubEnableEvent;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubPlugin;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPluginException;
import net.ME1312.SubServers.Host.Library.Log.FileLogger;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SubServers.Host Main Class
 */
public final class SubServers {
    public HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    public SubCreator creator;

    public Logger log;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLSection host = null;
    public YAMLSection lang = null;
    public SubDataClient subdata = null;

    public final Version version = new Version("2.11.2a");
    public final Version bversion = new Version(3);
    public final SubAPI api = new SubAPI(this);

    private boolean running;

    /**
     * SubServers.Host Launch
     *
     * @param args Args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new SubServers(args);
    }
    private SubServers(String[] args) {
        log = new Logger("SubServers");
        try {
            Logger.setup(System.out, System.err, dir);
            log.info.println("Loading SubServers v" + version.toString() + " Libraries... ");
            dir.mkdirs();
            new File(dir, "Plugins").mkdir();
            if (!(new UniversalFile(dir, "config.yml").exists())) {
                Util.copyFromJar(SubServers.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Created ~/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(SubServers.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Updated ~/config.yml");
            }

            if (!(new UniversalFile(dir, "Templates:Spigot Plugins").exists())) {
                new UniversalFile(dir, "Templates:Spigot Plugins").mkdirs();
                System.out.println("SubServers > Created ~/Templates/Spigot Plugins");
            }
            if (!(new UniversalFile(dir, "Templates:Sponge Config").exists())) {
                new UniversalFile(dir, "Templates:Sponge Config").mkdir();
                System.out.println("SubServers > Created ~/Templates/Sponge Config");
            }
            if (!(new UniversalFile(dir, "Templates:Sponge Mods").exists())) {
                new UniversalFile(dir, "Templates:Sponge Mods").mkdir();
                System.out.println("SubServers > Created ~/Templates/Sponge Mods");
            }

            if (new UniversalFile(dir, "Recently Deleted").exists()) {
                int f = new UniversalFile(dir, "Recently Deleted").listFiles().length;
                for (File file : new UniversalFile(dir, "Recently Deleted").listFiles()) {
                    try {
                        if (file.isDirectory()) {
                            if (new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json").exists()) {
                                JSONObject json = new JSONObject(Util.readAll(new FileReader(new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json"))));
                                if (json.keySet().contains("Timestamp")) {
                                    if (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTime().getTime() - json.getLong("Timestamp")) >= 7) {
                                        Util.deleteDirectory(file);
                                        f--;
                                        System.out.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Util.deleteDirectory(file);
                                    f--;
                                    System.out.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                System.out.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                            }
                        } else {
                            Files.delete(file.toPath());
                            f--;
                            System.out.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (f <= 0) {
                    Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
                }
            }

            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));
            subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", "undefined"),
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]));
            creator = new SubCreator(this);

            if (System.getProperty("subservers.host.plugins", "").length() > 0) {
                long begin = Calendar.getInstance().getTime().getTime();
                long i = 0;
                log.info.println("Loading SubAPI Plugins...");

                /*
                 * Decode Plugin List Variable
                 */
                String decoded = URLDecoder.decode(System.getProperty("subservers.host.plugins"), "UTF-8");
                List<String> classes = new LinkedList<String>();
                HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
                if (!decoded.contains(" ")) {
                    classes.add(decoded);
                } else {
                    classes.addAll(Arrays.asList(decoded.split(" ")));
                }

                /*
                 * Load Main Classes & Plugin Descriptions
                 */
                for (String main : classes) {
                    try {
                        Class<?> clazz = Class.forName(main);
                        if (!clazz.isAnnotationPresent(SubPlugin.class)) throw new ClassCastException("Cannot find plugin descriptor");

                        Object obj = clazz.getConstructor().newInstance();
                        try {
                            SubPluginInfo plugin = new SubPluginInfo(this, obj, clazz.getAnnotation(SubPlugin.class).name(), new Version(clazz.getAnnotation(SubPlugin.class).version()),
                                    Arrays.asList(clazz.getAnnotation(SubPlugin.class).authors()), (clazz.getAnnotation(SubPlugin.class).description().length() > 0)?clazz.getAnnotation(SubPlugin.class).description():null,
                                    (clazz.getAnnotation(SubPlugin.class).website().length() > 0)?new URL(clazz.getAnnotation(SubPlugin.class).website()):null, Arrays.asList(clazz.getAnnotation(SubPlugin.class).loadBefore()),
                                    Arrays.asList(clazz.getAnnotation(SubPlugin.class).dependencies()), Arrays.asList(clazz.getAnnotation(SubPlugin.class).softDependencies()));
                            if (plugins.keySet().contains(plugin.getName().toLowerCase())) log.warn.println("Duplicate plugin: " + plugin.getName().toLowerCase());
                            plugin.addExtra("subservers.plugin.loadafter", new ArrayList<String>());
                            plugins.put(plugin.getName().toLowerCase(), plugin);
                        } catch (Throwable e) {
                            log.error.println(new IllegalPluginException(e, "Cannot load plugin descriptor for main class: " + main));
                        }
                    } catch (ClassCastException e) {
                        log.error.println(new IllegalPluginException(e, "Main class isn't annotated as a SubPlugin: " + main));
                    } catch (InvocationTargetException e) {
                        log.error.println(new IllegalPluginException(e.getTargetException(), "Uncaught exception occurred while loading main class: " + main));
                    } catch (Throwable e) {
                        log.error.println(new IllegalPluginException(e, "Cannot load main class: " + main));
                    }
                }

                /*
                 * Load Extra Plugin Settings
                 */
                for (SubPluginInfo plugin : plugins.values()) {
                    for (String loadbefore : plugin.getLoadBefore()) {
                        if (plugins.keySet().contains(loadbefore.toLowerCase())) {
                            List<String> loadafter = plugins.get(loadbefore.toLowerCase()).getExtra("subservers.plugin.loadafter").asRawStringList();
                            loadafter.add(plugin.getName().toLowerCase());
                            plugins.get(loadbefore.toLowerCase()).addExtra("subservers.plugin.loadafter", loadafter);
                        }
                    }
                }

                /*
                 * Register Plugins
                 */
                while (plugins.size() > 0) {
                    List<String> loaded = new ArrayList<String>();
                    for (SubPluginInfo plugin : plugins.values()) {
                        try {
                            boolean load = true;
                            for (String depend : plugin.getDependancies()) {
                                if (plugins.keySet().contains(depend.toLowerCase())) {
                                    load = false;
                                } else if (!api.plugins.keySet().contains(depend.toLowerCase())) {
                                    throw new IllegalPluginException(new IllegalStateException("Unknown dependency: " + depend), "Cannot meet requirements for plugin: " + plugin.getName() + " v" + plugin.getVersion().toString());
                                }
                            }
                            for (String softdepend : plugin.getSoftDependancies()) {
                                if (plugins.keySet().contains(softdepend.toLowerCase())) {
                                    load = false;
                                }
                            }
                            for (String loadafter : plugin.getExtra("subservers.plugin.loadafter").asRawStringList()) {
                                if (plugins.keySet().contains(loadafter.toLowerCase())) {
                                    load = false;
                                }
                            }
                            if (load) try {
                                plugin.removeExtra("subservers.plugin.loadafter");
                                plugin.setEnabled(true);
                                api.addListener(plugin, plugin.get());
                                api.plugins.put(plugin.getName().toLowerCase(), plugin);
                                loaded.add(plugin.getName().toLowerCase());
                                log.info.println("Loaded " + plugin.getName() + " v" + plugin.getVersion().toString() + " by " + plugin.getAuthors().toString().substring(1, plugin.getAuthors().toString().length() - 1));
                                i++;
                            } catch (Throwable e) {
                                plugin.setEnabled(false);
                                throw new InvocationTargetException(e, "Problem loading plugin: " + plugin.getName());
                            }
                        } catch (InvocationTargetException e) {
                            log.error.println(e);
                            loaded.add(plugin.getName().toLowerCase());
                        }
                    }
                    int progress = 0;
                    for (String name : loaded) {
                        progress++;
                        plugins.remove(name);
                    }
                    if (progress == 0 && plugins.size() != 0) {
                        log.error.println(new IllegalStateException("Couldn't load more plugins but there are " + plugins.size() + " more"));
                        break;
                    }
                }

                /*
                 * Enable Plugins
                 */
                api.executeEvent(new SubEnableEvent(this));
                log.info.println(i + " Plugin"+((i == 1)?"":"s") + " loaded in " + (Calendar.getInstance().getTime().getTime() - begin) + "ms");
            }

            loadDefaults();

            running = true;
            loop();
        } catch (SocketException e) {
            log.severe.println(e);
        } catch (Exception e) {
            log.error.println(e);
        }
    }

    private void loop() {
        Scanner console = new Scanner(System.in);

        while (running && console.hasNextLine()) {
            if (!running) continue;
            final String umsg = console.nextLine();
            final CommandPreProcessEvent event;
            api.executeEvent(event = new CommandPreProcessEvent(this, umsg));
            if (!event.isCancelled()) {
                final String cmd = (umsg.startsWith("/"))?((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0].substring(1)):((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0]);
                if (api.commands.keySet().contains(cmd.toLowerCase())) {
                    ArrayList<String> args = new ArrayList<String>();
                    args.addAll(Arrays.asList(umsg.contains(" ") ? umsg.split(" ") : new String[]{umsg}));
                    args.remove(0);

                    new Thread(() ->{
                        try {
                            api.commands.get(cmd.toLowerCase()).command(cmd, args.toArray(new String[args.size()]));
                        } catch (Exception e) {
                            log.error.println(new InvocationTargetException(e, "Uncaught exception while running command"));
                        }
                    }).start();
                } else {
                    log.message.println("Unknown Command - " + umsg);
                }
            }
        }
    }

    private void loadDefaults() {
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                if (args.length == 0) {
                    log.message.println(
                            System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',',
                            "Java " + System.getProperty("java.version") + ',',
                            "SubServers.Host v" + version.toString() + ((bversion == null) ? "" : " BETA " + bversion.toString()));
                } else if (api.plugins.get(args[0].toLowerCase()) != null) {
                    SubPluginInfo plugin = api.plugins.get(args[0].toLowerCase());
                    log.message.println(plugin.getName() + " v" + plugin.getVersion() + " by " + plugin.getAuthors().toString().substring(1, plugin.getAuthors().toString().length() - 1));
                    if (plugin.getWebsite() != null) log.message.println(plugin.getWebsite().toString());
                    if (plugin.getDescription() != null) log.message.println("", plugin.getDescription());
                } else {
                    log.message.println("There is no plugin with that name");
                }
            }
        }.usage("[plugin]").description("Gets the version of the System and SubServers or the specified Plugin").help(
                "This command will print what OS you're running, your OS version,",
                "your Java version, and the SubServers.Host version.",
                "",
                "If the [plugin] option is provided, it will print information about the specified plugin instead.",
                "",
                "Examples:",
                "  /version",
                "  /version ExamplePlugin"
        ).register("ver", "version");
        new Command(null) {
            public void command(String handle, String[] args) {
                HashMap<String, String> commands = new LinkedHashMap<String, String>();
                HashMap<Command, String> handles = new LinkedHashMap<Command, String>();

                int length = 0;
                for(String command : api.commands.keySet()) {
                    String formatted = "/ ";
                    Command cmd = api.commands.get(command);
                    String alias = (handles.keySet().contains(cmd))?handles.get(cmd):null;

                    if (alias != null) formatted = commands.get(alias);
                    if (cmd.usage().length == 0 || alias != null) {
                        formatted = formatted.replaceFirst("\\s", ((alias != null)?"|":"") + command + ' ');
                    } else {
                        String usage = "";
                        for (String str : cmd.usage()) usage += ((usage.length() == 0)?"":" ") + str;
                        formatted = formatted.replaceFirst("\\s", command + ' ' + usage + ' ');
                    }
                    if(formatted.length() > length) {
                        length = formatted.length();
                    }

                    if (alias == null) {
                        commands.put(command, formatted);
                        handles.put(cmd, command);
                    } else {
                        commands.put(alias, formatted);
                    }
                }

                if (args.length == 0) {
                    log.message.println("SubServers.Host Command List:");
                    for (String command : commands.keySet()) {
                        String formatted = commands.get(command);
                        Command cmd = api.commands.get(command);

                        while (formatted.length() < length) {
                            formatted += ' ';
                        }
                        formatted += ((cmd.description() == null || cmd.description().length() == 0)?"  ":"- "+cmd.description());

                        log.message.println(formatted);
                    }
                } else if (api.commands.keySet().contains((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase())) {
                    Command cmd = api.commands.get((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase());
                    String formatted = commands.get(Util.getBackwards(api.commands, cmd).get(0));
                    log.message.println(formatted.substring(0, formatted.length() - 1));
                    for (String line : cmd.help()) {
                        log.message.println("  " + line);
                    }
                } else {
                    log.message.println("There is no command with that name");
                }
            }
        }.usage("[command]").description("Prints a list of the commands and/or their descriptions").help(
                "This command will print a list of all currently registered commands and aliases,",
                "along with their usage and a short description.",
                "",
                "If the [command] option is provided, it will print that command, it's aliases,",
                "it's usage, and an extended description like the one you see here instead.",
                "",
                "Examples:",
                "  /help",
                "  /help end"
        ).register("help", "?");
        new Command(null) {
            @Override
            public void command(String handle, String[] args) {
                stop(0);
            }
        }.description("Stops this SubServers instance").help(
                "This command will shutdown this instance of SubServers.Host,",
                "SubServers running on this host, and any plugins currently running via SubAPI.",
                "",
                "Example:",
                "  /stop"
        ).register("exit", "end");
    }

    /**
     * Stop SubServers.Host
     *
     * @param exit Exit Code
     */
    public void stop(int exit) {
        log.info.println("Shutting down...");
        running = false;
        SubDisableEvent event = new SubDisableEvent(this, exit);
        api.executeEvent(event);

        List<String> subservers = new ArrayList<String>();
        subservers.addAll(servers.keySet());

        for (String server : subservers) {
            servers.get(server).stop();
            try {
                servers.get(server).waitFor();
            } catch (Exception e) {
                log.error.println(e);
            }
        }
        subservers.clear();
        servers.clear();

        if (creator.isBusy()) {
            creator.terminate();
            try {
                creator.waitFor();
            } catch (Exception e) {
                log.error.println(e);
            }
        }

        try {
            Thread.sleep(500);
        } catch (Exception e) {
            log.error.println(e);
        }
        if (subdata != null) Util.isException(() -> subdata.destroy(false));

        Util.isException(FileLogger::end);
        System.exit(event.getExitCode());
    }
}