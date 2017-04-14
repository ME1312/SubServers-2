package net.ME1312.SubServers.Host;

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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public final Version version = new Version("2.11.2b");
    public final Version bversion = null;
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

            if (!(new UniversalFile(dir, "Templates").exists())) {
                unzip(SubServers.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/templates.zip"), dir);
                System.out.println("SubServers > Created ~/Templates");
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
                                        log.info.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Util.deleteDirectory(file);
                                    f--;
                                    log.info.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                log.info.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                            }
                        } else {
                            Files.delete(file.toPath());
                            f--;
                            log.info.println("SubServers > Removed ~/Recently Deleted/" + file.getName());
                        }
                    } catch (Exception e) {
                        log.error.println(e);
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
        SubCommand.load(this);
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

    private void unzip(InputStream zip, File dir) {
        byte[] buffer = new byte[1024];
        try{
            ZipInputStream zis = new ZipInputStream(zip);
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File newFile = new File(dir + File.separator + ze.getName());
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                } else if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }
            zis.closeEntry();
            zis.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}