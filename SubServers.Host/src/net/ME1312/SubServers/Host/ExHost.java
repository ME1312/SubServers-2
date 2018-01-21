package net.ME1312.SubServers.Host;

import jline.console.ConsoleReader;
import net.ME1312.SubServers.Host.API.Event.CommandPreProcessEvent;
import net.ME1312.SubServers.Host.API.Event.SubDisableEvent;
import net.ME1312.SubServers.Host.API.Event.SubEnableEvent;
import net.ME1312.SubServers.Host.API.Event.SubReloadEvent;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubPlugin;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPluginException;
import net.ME1312.SubServers.Host.Library.Log.FileLogger;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.PluginClassLoader;
import net.ME1312.SubServers.Host.Library.NamedContainer;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Cipher;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * SubServers.Host Main Class
 */
public final class ExHost {
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public HashMap<String, SubCreator.ServerTemplate> templates = new HashMap<String, SubCreator.ServerTemplate>();
    public HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    public SubCreator creator;

    public Logger log;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLSection host = null;
    public SubDataClient subdata = null;

    public final Version version = new Version("2.13a");
    public final Version bversion = new Version(2);
    public final SubAPI api = new SubAPI(this);

    private ConsoleReader jline;
    private boolean running = false;
    private boolean ready = false;

    /**
     * SubServers.Host Launch
     *
     * @param args Args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (System.getProperty("RM.subservers", "true").equalsIgnoreCase("true")) {
            new ExHost(args);
        } else {
            System.out.println(">> SubServers code has been disallowed to work on this machine");
            System.out.println(">> Check with your provider for more information");
            System.exit(1);
        }
    }

    private ExHost(String[] args) {
        try {
            JarFile jarFile = new JarFile(new File(ExHost.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            Enumeration<JarEntry> entries = jarFile.entries();

            boolean isplugin = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    api.knownClasses.add(entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.'));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        log = new Logger("SubServers");
        try {
            jline = new ConsoleReader(System.in, AnsiConsole.out());
            Logger.setup(AnsiConsole.out(), AnsiConsole.err(), jline, dir);
            log.info.println("Loading SubServers.Host v" + version.toString() + " Libraries");
            dir.mkdirs();
            new File(dir, "Plugins").mkdir();
            if (!(new UniversalFile(dir, "config.yml").exists())) {
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Created ~/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Updated ~/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));

            if (!(new UniversalFile(dir, "Templates").exists())) {
                new UniversalFile(dir, "Templates").mkdir();
                log.info.println("Created ~/Templates/");
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
                                        log.info.println("Removed ~/Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Util.deleteDirectory(file);
                                    f--;
                                    log.info.println("Removed ~/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                log.info.println("Removed ~/Recently Deleted/" + file.getName());
                            }
                        } else {
                            Files.delete(file.toPath());
                            f--;
                            log.info.println("Removed ~/Recently Deleted/" + file.getName());
                        }
                    } catch (Exception e) {
                        log.error.println(e);
                    }
                }
                if (f <= 0) {
                    Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
                }
            }
            running = true;
            Cipher cipher = null;
            if (!config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equalsIgnoreCase("NONE")) {
                if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                    log.info.println("Cannot encrypt connection without a password");
                } else if (!SubDataClient.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                    log.info.println("Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                } else {
                    cipher = SubDataClient.getCipher(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                }
            }
            subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", "undefined"),
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), cipher);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (running) {
                    log.warn.println("Received request from system to shutdown");
                    forcequit(0);
                }
            }));
            creator = new SubCreator(this);

            UniversalFile pldir = new UniversalFile(dir, "Plugins");
            if (pldir.exists() && pldir.listFiles().length > 0) {
                long begin = Calendar.getInstance().getTime().getTime();
                log.info.println("Loading SubAPI Plugins...");

                /*
                 * Find Jars
                 */
                LinkedList<URL> jars = new LinkedList<URL>();
                for (File file : pldir.listFiles()) {
                    if (file.getName().endsWith(".jar")) {
                        jars.add(file.toURI().toURL());
                    }
                }

                /*
                 * Find & Pre-Load Main Classes
                 * (Unordered)
                 */
                URLClassLoader superloader = new URLClassLoader(jars.toArray(new URL[jars.size()]), this.getClass().getClassLoader());
                LinkedHashMap<String, NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>> classes = new LinkedHashMap<String, NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>>();
                for (File file : pldir.listFiles()) {
                    if (file.getName().endsWith(".jar")) {
                        try {
                            JarFile jar = new JarFile(file);
                            Enumeration<JarEntry> entries = jar.entries();
                            LinkedList<String> mains = new LinkedList<String>();
                            List<String> contents = new ArrayList<String>();

                            if (jar.getJarEntry("package.xml") != null) {
                                try {
                                    NodeList xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(jar.getInputStream(jar.getJarEntry("package.xml"))).getElementsByTagName("class");
                                    if (xml.getLength() > 0) {
                                        for (int i = 0; i < xml.getLength(); i++) {
                                            mains.add(xml.item(i).getTextContent());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error.println(new IllegalPluginException(e, "Couldn't load package.xml for " + file.getName()));
                                }
                            } else {
                                log.error.println(new IllegalPluginException(new FileNotFoundException(), "Couldn't find package.xml for " + file.getName()));
                            }

                            boolean isplugin = false;
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                    String cname = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                                    contents.add(cname);
                                    if (mains.contains(cname)) {
                                        try {
                                            Class<?> clazz = superloader.loadClass(cname);
                                            if (clazz.isAnnotationPresent(SubPlugin.class)) {
                                                NamedContainer<LinkedList<String>, LinkedHashMap<String, String>> jarmap = (classes.keySet().contains(file.getName()))?classes.get(file.getName()):new NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>(new LinkedList<String>(), new LinkedHashMap<>());
                                                for (String dependancy : clazz.getAnnotation(SubPlugin.class).dependencies()) jarmap.name().add(dependancy);
                                                for (String dependancy : clazz.getAnnotation(SubPlugin.class).softDependencies()) jarmap.name().add(dependancy);
                                                jarmap.get().put(clazz.getAnnotation(SubPlugin.class).name(), cname);
                                                classes.put(file.getName(), jarmap);
                                                isplugin = true;
                                                mains.remove(cname);
                                            } else {
                                                log.error.println(new IllegalPluginException(new ClassCastException(), "Main class isn't annotated as a SubPlugin: " + cname));
                                            }
                                        } catch (Throwable e) {
                                            log.error.println(new IllegalPluginException(e, "Couldn't load main class: " + cname));
                                        }
                                    }
                                }
                            }

                            for (String main : mains) {
                                log.error.println(new IllegalPluginException(new ClassNotFoundException(), "Couldn't find main class: " + main));
                            }

                            if (isplugin) api.knownClasses.addAll(contents);
                            jar.close();
                        } catch (Throwable e) {
                            log.error.println(new InvocationTargetException(e, "Problem searching plugin jar: " + file.getName()));
                        }
                    }
                }
                superloader.close();

                /*
                 * Load Main Classes & Plugin Descriptions
                 * (Ordered by Known Dependencies)
                 */
                int progress = 1;
                HashMap<String, PluginClassLoader> loaders = new HashMap<String, PluginClassLoader>();
                HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
                while (classes.size() > 0) {
                    LinkedHashMap<String, LinkedList<String>> loaded = new LinkedHashMap<String, LinkedList<String>>();
                    for (String jar : classes.keySet()) {
                        LinkedList<String> loadedlist = new LinkedList<String>();
                        if (!loaders.keySet().contains(jar)) loaders.put(jar, new PluginClassLoader(this.getClass().getClassLoader(), new File(pldir, jar).toURI().toURL()));
                        for (String name : classes.get(jar).get().keySet()) {
                            boolean load = true;
                            for (String depend : classes.get(jar).name()) {
                                if (!plugins.keySet().contains(depend.toLowerCase())) {
                                    load = progress <= 0;
                                }
                            }

                            if (load) {
                                String main = classes.get(jar).get().get(name);
                                try {
                                    Class<?> clazz = loaders.get(jar).loadClass(main);
                                    if (!clazz.isAnnotationPresent(SubPlugin.class))
                                        throw new ClassCastException("Cannot find plugin descriptor");

                                    Object obj = clazz.getConstructor().newInstance();
                                    try {
                                        SubPluginInfo plugin = new SubPluginInfo(this, obj, clazz.getAnnotation(SubPlugin.class).name(), new Version(clazz.getAnnotation(SubPlugin.class).version()),
                                                Arrays.asList(clazz.getAnnotation(SubPlugin.class).authors()), (clazz.getAnnotation(SubPlugin.class).description().length() > 0) ? clazz.getAnnotation(SubPlugin.class).description() : null,
                                                (clazz.getAnnotation(SubPlugin.class).website().length() > 0) ? new URL(clazz.getAnnotation(SubPlugin.class).website()) : null, Arrays.asList(clazz.getAnnotation(SubPlugin.class).loadBefore()),
                                                Arrays.asList(clazz.getAnnotation(SubPlugin.class).dependencies()), Arrays.asList(clazz.getAnnotation(SubPlugin.class).softDependencies()));
                                        if (plugins.keySet().contains(plugin.getName().toLowerCase()))
                                            log.warn.println("Duplicate plugin: " + plugin.getName().toLowerCase());
                                        plugin.addExtra("subservers.plugin.loadafter", new ArrayList<String>());
                                        plugins.put(plugin.getName().toLowerCase(), plugin);
                                    } catch (Throwable e) {
                                        log.error.println(new IllegalPluginException(e, "Cannot load plugin descriptor for main class: " + main));
                                    }
                                } catch(ClassCastException e) {
                                    log.error.println(new IllegalPluginException(e, "Main class isn't annotated as a SubPlugin: " + main));
                                } catch(InvocationTargetException e) {
                                    log.error.println(new IllegalPluginException(e.getTargetException(), "Uncaught exception occurred while loading main class: " + main));
                                } catch(Throwable e) {
                                    log.error.println(new IllegalPluginException(e, "Couldn't load main class: " + main));
                                }
                                loadedlist.add(name);
                            }
                        }
                        if (loadedlist.size() > 0) loaded.put(jar, loadedlist);
                    }
                    progress = 0;
                    for (String jar : loaded.keySet()) {
                        NamedContainer<LinkedList<String>, LinkedHashMap<String, String>> jarmap = classes.get(jar);
                        progress++;
                        for (String main : loaded.get(jar)) jarmap.get().remove(main);
                        if (jarmap.get().size() > 0) {
                            classes.put(jar, jarmap);
                        } else {
                            classes.remove(jar);
                        }
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
                 * (Ordered by LoadBefore & Dependencies)
                 */
                int i = 0;
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
                    progress = 0;
                    for (String name : loaded) {
                        progress++;
                        plugins.remove(name);
                    }
                    if (progress == 0 && plugins.size() != 0) {
                        log.error.println(new IllegalStateException("Couldn't load more plugins yet " + plugins.size() + " remain unloaded"));
                        break;
                    }
                }

                /*
                 * Enable Plugins
                 */
                api.executeEvent(new SubEnableEvent(this));
                log.info.println(i + " Plugin"+((i == 1)?"":"s") + " loaded in " + new DecimalFormat("0.000").format((Calendar.getInstance().getTime().getTime() - begin) / 1000D) + "s");
            }

            loadDefaults();

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Host/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                        NodeList updnodeList = updxml.getElementsByTagName("version");
                        Version updversion = version;
                        int updcount = -1;
                        for (int i = 0; i < updnodeList.getLength(); i++) {
                            Node node = updnodeList.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                if (!node.getTextContent().startsWith("-") && new Version(node.getTextContent()).compareTo(updversion) >= 0) {
                                    updversion = new Version(node.getTextContent());
                                    updcount++;
                                }
                            }
                        }
                        if (!updversion.equals(version)) log.info.println("SubServers.Host v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                    } catch (Exception e) {}
                }
            }, 0, TimeUnit.DAYS.toMillis(2));

            loop();
        } catch (Exception e) {
            log.error.println(e);
            forcequit(1);
        }
    }

    public void reload() throws IOException {
        if (subdata != null)
            subdata.destroy(0);

        config.reload();

        Cipher cipher = null;
        if (!config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equalsIgnoreCase("NONE")) {
            if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                log.info.println("Cannot encrypt connection without a password");
            } else if (!SubDataClient.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                log.info.println("Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
            } else {
                cipher = SubDataClient.getCipher(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
            }
        }
        subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", "undefined"),
                InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), cipher);

        SubAPI.getInstance().executeEvent(new SubReloadEvent(this));
    }

    private void loop() throws Exception {
        String umsg;
        ready = true;
        while (ready && (umsg = jline.readLine(">")) != null) {
            if (!ready || umsg.equals("")) continue;
            final CommandPreProcessEvent event;
            api.executeEvent(event = new CommandPreProcessEvent(this, umsg));
            if (!event.isCancelled()) {
                final String cmd = (umsg.startsWith("/"))?((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0].substring(1)):((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0]);
                if (api.commands.keySet().contains(cmd.toLowerCase())) {
                    ArrayList<String> args = new ArrayList<String>();
                    args.addAll(Arrays.asList(umsg.contains(" ") ? umsg.split(" ") : new String[]{umsg}));
                    args.remove(0);
                    try {
                        api.commands.get(cmd.toLowerCase()).command(cmd, args.toArray(new String[args.size()]));
                    } catch (Exception e) {
                        log.error.println(new InvocationTargetException(e, "Uncaught exception while running command"));
                    }
                } else {
                    log.message.println("Unknown Command - " + umsg);
                }
                jline.getOutput().write("\b \b");
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
        if (ready) {
            log.info.println("Shutting down...");
            SubDisableEvent event = new SubDisableEvent(this, exit);
            api.executeEvent(event);

            forcequit(event.getExitCode());
        }
    } private void forcequit(int exit) {
        if (ready) {
            ready = false;

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
            servers.clear();

            if (creator != null) {
                creator.terminate();
                try {
                    creator.waitFor();
                } catch (Exception e) {
                    log.error.println(e);
                }
            }
            running = false;

            try {
                Thread.sleep(500);
            } catch (Exception e) {
                log.error.println(e);
            }
            if (subdata != null) Util.isException(() -> subdata.destroy(0));

            Util.isException(FileLogger::end);
            System.exit(exit);
        }
    }
}