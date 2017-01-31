package net.ME1312.SubServers.Host;

import net.ME1312.SubServers.Host.API.Event.CommandPreProcessEvent;
import net.ME1312.SubServers.Host.API.Event.SubDisableEvent;
import net.ME1312.SubServers.Host.API.Event.SubEnableEvent;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubPlugin;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPluginException;
import net.ME1312.SubServers.Host.Library.Log.FileLogger;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.SubDataClient;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;

/**
 * SubServers.Host Main Class
 */
public final class SubServers {
    public Logger log;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLSection lang = null;
    public SubDataClient subdata = null;

    public final Version version = new Version("2.11.2a");
    public final Version bversion = new Version(2);
    public final SubAPI api = new SubAPI(this);

    private boolean running;

    public static void main(String[] args) throws Exception {
        new SubServers(args);
    }

    private SubServers(String[] args) {
        log = new Logger("SubServers");
        try {
            Logger.setup(System.out, System.err, dir);
            log.info("Loading SubServers v" + version.toString() + " Libraries... ");
            dir.mkdirs();
            new File(dir, "Plugins").mkdir();
            if (!(new UniversalFile(dir, "config.yml").exists())) {
                Util.copyFromJar(SubServers.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info("Created ~/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(SubServers.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info("Updated ~/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));
            subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", "undefined"),
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]));

            if (System.getProperty("subservers.host.plugins", "").length() > 0) {
                long begin = Calendar.getInstance().getTime().getTime();
                long i = 0;
                log.info("Loading SubAPI Plugins...");
                String decoded = URLDecoder.decode(System.getProperty("subservers.host.plugins"), "UTF-8");
                List<String> classes = new LinkedList<String>();
                HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
                if (!decoded.contains(" ")) {
                    classes.add(decoded);
                } else {
                    classes.addAll(Arrays.asList(decoded.split(" ")));
                }
                for (String main : classes) {
                    try {
                        Class<?> clazz = Class.forName(main);
                        if (!clazz.isAnnotationPresent(SubPlugin.class)) throw new ClassCastException("Cannot find plugin descriptor");

                        Object obj = clazz.getConstructor().newInstance();
                        try {
                            SubPluginInfo plugin = new SubPluginInfo(obj, clazz.getAnnotation(SubPlugin.class).name(), new Version(clazz.getAnnotation(SubPlugin.class).version()),
                                    Arrays.asList(clazz.getAnnotation(SubPlugin.class).authors()), (clazz.getAnnotation(SubPlugin.class).description().length() > 0)?clazz.getAnnotation(SubPlugin.class).description():null,
                                    (clazz.getAnnotation(SubPlugin.class).website().length() > 0)?new URL(clazz.getAnnotation(SubPlugin.class).website()):null, Arrays.asList(clazz.getAnnotation(SubPlugin.class).depend()),
                                    Arrays.asList(clazz.getAnnotation(SubPlugin.class).softDepend()));
                            if (plugins.keySet().contains(plugin.getName().toLowerCase())) log.warn("Duplicate plugin: " + plugin.getName());
                            plugins.put(plugin.getName().toLowerCase(), plugin);
                        } catch (Throwable e) {
                            log.error(new IllegalPluginException(e, "Cannot load plugin descriptor for main class: " + main));
                        }
                    } catch (ClassCastException e) {
                        log.error(new IllegalPluginException(e, "Main class isn't annotated as a SubPlugin: " + main));
                    } catch (InvocationTargetException e) {
                        log.error(new IllegalPluginException(e.getTargetException(), "Uncaught exception occurred while loading main class: " + main));
                    } catch (Throwable e) {
                        log.error(new IllegalPluginException(e, "Cannot load main class: " + main));
                    }
                }

                while (plugins.size() > 0) {
                    List<String> loaded = new ArrayList<String>();
                    for (String name : plugins.keySet()) {
                        SubPluginInfo plugin = plugins.get(name);
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
                            if (load) {
                                try {
                                    plugin.setEnabled(true);
                                    api.addListener(plugin, plugin.get());
                                    api.plugins.put(plugin.getName().toLowerCase(), plugin);
                                    loaded.add(name);
                                    log.info("Loaded " + plugin.getName() + " v" + plugin.getVersion().toString());
                                    i++;
                                } catch (Throwable e) {
                                    plugin.setEnabled(false);
                                    throw new InvocationTargetException(e, "Problem enabling plugin: " + plugin.getName() + " v" + plugin.getVersion().toString() + " (is it up to date?)");
                                }
                            }
                        } catch (InvocationTargetException e) {
                            log.error(e);
                            loaded.add(name);
                        }
                    }
                    int progress = 0;
                    for (String name : loaded) {
                        progress++;
                        plugins.remove(name);
                    }
                    if (progress == 0 && plugins.size() != 0) {
                        log.error(new IllegalStateException("Cannot load any more plugins but there's " + plugins.size() + " left"));
                        break;
                    }
                }

                api.runEvent(new SubEnableEvent(this));
                log.info("SubServers > " + i + " Plugin"+((i == 1)?"":"s") + " loaded in " + (Calendar.getInstance().getTime().getTime() - begin) + "ms");
            }

            api.addCommand((command, cargs) -> log.info(
                    System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',',
                    "Java " + System.getProperty("java.version") + ',',
                    "SubServers.Host v" + version.toString() + ((bversion == null)?"":" BETA " + bversion.toString())), "ver", "version");
            api.addCommand((command, cargs) -> stop(0), "stop", "exit");

            running = true;
            loop();
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void loop() {
        Scanner console = new Scanner(System.in);

        while (running && console.hasNextLine()) {
            final String umsg = console.nextLine();
            final CommandPreProcessEvent event;
            api.runEvent(event = new CommandPreProcessEvent(this, umsg));
            if (!event.isCancelled()) {
                final String cmd = (umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0];
                if (api.commands.keySet().contains(cmd.toLowerCase())) {
                    ArrayList<String> args = new ArrayList<String>();
                    args.addAll(Arrays.asList(umsg.contains(" ") ? umsg.split(" ") : new String[]{umsg}));
                    args.remove(0);

                    new Thread(() ->{
                        try {
                            api.commands.get(cmd.toLowerCase()).command(cmd, args.toArray(new String[args.size()]));
                        } catch (Exception e) {
                            log.error(new InvocationTargetException(e, "Uncaught exception while running command"));
                        }
                    }).start();
                } else {
                    log.info("Unknown Command - " + umsg);
                }
            }
        }
    }

    public void stop(int exit) {
        log.info("Shutting down...");
        SubDisableEvent event = new SubDisableEvent(this, exit);
        api.runEvent(event);
        running = false;
        if (subdata != null) Util.isException(() -> subdata.destroy(false));

        Util.isException(FileLogger::end);
        System.exit(event.getExitCode());
    }
}