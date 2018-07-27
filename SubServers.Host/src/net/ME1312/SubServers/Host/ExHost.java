package net.ME1312.SubServers.Host;

import jline.console.ConsoleReader;
import net.ME1312.SubServers.Host.API.Event.*;
import net.ME1312.SubServers.Host.API.SubPluginInfo;
import net.ME1312.SubServers.Host.API.SubPlugin;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.*;
import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPluginException;
import net.ME1312.SubServers.Host.Library.Log.FileLogger;
import net.ME1312.SubServers.Host.Library.Log.Logger;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Library.Version.VersionType;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubServers.Host Main Class
 */
public final class ExHost {
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public HashMap<String, SubCreator.ServerTemplate> templates = new HashMap<String, SubCreator.ServerTemplate>();
    public HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    public SubCreator creator;

    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public Logger log;
    public YAMLConfig config;
    public YAMLSection host = null;
    public SubDataClient subdata = null;

    public final SubAPI api = new SubAPI(this);
    //public static final Version version = Version.fromString("2.13a/pr5");
    public static final Version version = new Version(Version.fromString("2.13a/pr5"), VersionType.SNAPSHOT, (ExHost.class.getPackage().getSpecificationTitle() == null)?"custom":ExHost.class.getPackage().getSpecificationTitle()); // TODO Snapshot Version

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
                    build = f.get(version) != VersionType.SNAPSHOT && ExHost.class.getPackage().getSpecificationTitle() != null;
                    f.setAccessible(false);
                } catch (Exception e) {}

                System.out.println("");
                System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + ',');
                System.out.println("Java " + System.getProperty("java.version") + ",");
                System.out.println("SubServers.Host v" + version.toExtendedString() + ((build)?" (" + ExHost.class.getPackage().getSpecificationTitle() + ')':""));
                System.out.println("");
            } else {
                new ExHost(options);
            }
        } else {
            System.out.println(">> SubServers code has been disallowed to work on this machine");
            System.out.println(">> Check with your provider for more information");
            System.exit(1);
        }
    }

    private ExHost(joptsimple.OptionSet options) {
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
                                FileReader reader = new FileReader(new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json"));
                                JSONObject json = new JSONObject(Util.readAll(reader));
                                reader.close();
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

            /*
             * Find Jars
             */
            UniversalFile pldir = new UniversalFile(dir, "Plugins");
            LinkedList<File> pljars = new LinkedList<File>();
            if (pldir.exists() && pldir.isDirectory()) for (File file : pldir.listFiles()) {
                if (file.getName().endsWith(".jar")) pljars.add(file);
            }
            if (pljars.size() > 0) {
                long begin = Calendar.getInstance().getTime().getTime();
                log.info.println("Loading SubAPI Plugins...");

                /*
                 * Load Jars & Find Main Classes
                 * (Unordered)
                 */
                LinkedHashMap<PluginClassLoader, NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>> classes = new LinkedHashMap<PluginClassLoader, NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>>();
                for (File file : pljars) {
                    try {
                        JarFile jar = new JarFile(file);
                        Enumeration<JarEntry> entries = jar.entries();
                        PluginClassLoader loader = new PluginClassLoader(this.getClass().getClassLoader(), file.toURI().toURL());
                        List<String> contents = new ArrayList<String>();

                        loader.setDefaultClass(ClassNotFoundException.class);
                        boolean isplugin = false;
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                String cname = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                                contents.add(cname);
                                try {
                                    Class<?> clazz = loader.loadClass(cname);
                                    if (clazz.isAnnotationPresent(SubPlugin.class)) {
                                        NamedContainer<LinkedList<String>, LinkedHashMap<String, String>> jarmap = (classes.keySet().contains(loader))?classes.get(loader):new NamedContainer<LinkedList<String>, LinkedHashMap<String, String>>(new LinkedList<String>(), new LinkedHashMap<>());
                                        for (String dependancy : clazz.getAnnotation(SubPlugin.class).dependencies()) jarmap.name().add(dependancy);
                                        for (String dependancy : clazz.getAnnotation(SubPlugin.class).softDependencies()) jarmap.name().add(dependancy);
                                        jarmap.get().put(clazz.getAnnotation(SubPlugin.class).name(), cname);
                                        classes.put(loader, jarmap);
                                        isplugin = true;
                                    }
                                } catch (Throwable e) {
                                    log.error.println(new IllegalPluginException(e, "Couldn't load class: " + cname));
                                }
                            }
                        }
                        loader.setDefaultClass(null);

                        if (!isplugin) {
                            new PluginClassLoader(this.getClass().getClassLoader(), file.toURI().toURL());
                            log.info.println("Loaded Library: " + file.getName());
                        }
                        api.knownClasses.addAll(contents);
                        jar.close();
                    } catch (Throwable e) {
                        log.error.println(new InvocationTargetException(e, "Problem searching plugin jar: " + file.getName()));
                    }
                }

                /*
                 * Load Main Classes & Plugin Descriptions
                 * (Ordered by Known Dependencies)
                 */
                int progress = 1;
                HashMap<String, SubPluginInfo> plugins = new LinkedHashMap<String, SubPluginInfo>();
                while (classes.size() > 0) {
                    LinkedHashMap<PluginClassLoader, LinkedList<String>> loaded = new LinkedHashMap<PluginClassLoader, LinkedList<String>>();
                    for (PluginClassLoader loader : classes.keySet()) {
                        LinkedList<String> loadedlist = new LinkedList<String>();
                        for (String name : classes.get(loader).get().keySet()) {
                            boolean load = true;
                            for (String depend : classes.get(loader).name()) {
                                if (!plugins.keySet().contains(depend.toLowerCase())) {
                                    load = progress <= 0;
                                }
                            }

                            if (load) {
                                String main = classes.get(loader).get().get(name);
                                try {
                                    Class<?> clazz = loader.loadClass(main);
                                    if (!clazz.isAnnotationPresent(SubPlugin.class))
                                        throw new ClassCastException("Cannot find plugin descriptor");

                                    Object obj = clazz.getConstructor().newInstance();
                                    try {
                                        SubPluginInfo plugin = new SubPluginInfo(this, obj, clazz.getAnnotation(SubPlugin.class).name(), Version.fromString(clazz.getAnnotation(SubPlugin.class).version()),
                                                Arrays.asList(clazz.getAnnotation(SubPlugin.class).authors()), (clazz.getAnnotation(SubPlugin.class).description().length() > 0)?clazz.getAnnotation(SubPlugin.class).description():null,
                                                (clazz.getAnnotation(SubPlugin.class).website().length() > 0)?new URL(clazz.getAnnotation(SubPlugin.class).website()):null, Arrays.asList(clazz.getAnnotation(SubPlugin.class).loadBefore()),
                                                Arrays.asList(clazz.getAnnotation(SubPlugin.class).dependencies()), Arrays.asList(clazz.getAnnotation(SubPlugin.class).softDependencies()));
                                        if (plugins.keySet().contains(plugin.getName().toLowerCase()))
                                            log.warn.println("Duplicate plugin: " + plugin.getName().toLowerCase());
                                        plugin.addExtra("subservers.plugin.loadafter", new ArrayList<String>());
                                        plugins.put(plugin.getName().toLowerCase(), plugin);
                                    } catch (Throwable e) {
                                        log.error.println(new IllegalPluginException(e, "Couldn't load plugin descriptor for main class: " + main));
                                    }
                                } catch (ClassCastException e) {
                                    log.error.println(new IllegalPluginException(e, "Main class isn't annotated as a SubPlugin: " + main));
                                } catch (InvocationTargetException e) {
                                    log.error.println(new IllegalPluginException(e.getTargetException(), "Uncaught exception occurred while loading main class: " + main));
                                } catch (Throwable e) {
                                    log.error.println(new IllegalPluginException(e, "Couldn't load main class: " + main));
                                }
                                loadedlist.add(name);
                            }
                        }
                        if (loadedlist.size() > 0) loaded.put(loader, loadedlist);
                    }
                    progress = 0;
                    for (PluginClassLoader loader : loaded.keySet()) {
                        NamedContainer<LinkedList<String>, LinkedHashMap<String, String>> jarmap = classes.get(loader);
                        progress++;
                        for (String main : loaded.get(loader)) jarmap.get().remove(main);
                        if (jarmap.get().size() > 0) {
                            classes.put(loader, jarmap);
                        } else {
                            classes.remove(loader);
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
                                String a = "";
                                int ai = 0;
                                for (String author : plugin.getAuthors()) {
                                    ai++;
                                    if (ai > 1) {
                                        if (plugin.getAuthors().size() > 2) a += ", ";
                                        else if (plugin.getAuthors().size() == 2) a += ' ';
                                        if (ai == plugin.getAuthors().size()) a += "and ";
                                    }
                                    a += author;
                                }
                                log.info.println("Loaded " + plugin.getName() + " v" + plugin.getVersion().toString() + " by " + a);
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

            loadDefaults();

            new Metrics(this);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        YAMLSection tags = new YAMLSection(new JSONObject("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));

                        Version updversion = version;
                        int updcount = 0;
                        for (YAMLSection tag : tags.getSectionList("tags")) {
                            Version version = Version.fromString(tag.getString("ref").substring(10));
                            if (!version.equals(version) && version.compareTo(updversion) > 0) {
                                updversion = version;
                                updcount++;
                            }
                        }
                        if (updcount > 0) log.info.println("SubServers.Host v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                    } catch (Exception e) {}
                }
            }, 0, TimeUnit.DAYS.toMillis(2));

            if (!options.has("noconsole")) {
                loop();
            }
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
        String line;
        ready = true;
        while (ready && (line = jline.readLine(">")) != null) {
            if (!ready || line.replaceAll("\\s", "").length() == 0) continue;
            final CommandPreProcessEvent event;
            api.executeEvent(event = new CommandPreProcessEvent(this, line));
            if (!event.isCancelled()) {
                LinkedList<String> args = new LinkedList<String>();
                Matcher parser = Pattern.compile("(?:^|\\s+)(\"(?:\\\\\"|[^\"])+\"?|(?:\\\\\\s|[^\\s])+)").matcher(line);
                while (parser.find()) {
                    String arg = parser.group(1);
                    if (arg.startsWith("\"")) arg = arg.substring(1, arg.length() - ((arg.endsWith("\""))?1:0));
                    arg = unescapeCommand(arg);
                    args.add(arg);
                }
                String cmd = args.get(0);
                args.remove(0);
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                if (args.size() >= 1 &&
                       ((cmd.equalsIgnoreCase("sub") && !api.commands.keySet().contains("sub")) ||
                        (cmd.equalsIgnoreCase("subserver") && !api.commands.keySet().contains("subserver")) ||
                        (cmd.equalsIgnoreCase("subservers") && !api.commands.keySet().contains("subservers")))) {
                    cmd = args.get(0);
                    args.remove(0);
                }

                if (api.commands.keySet().contains(cmd.toLowerCase())) {
                    try {
                        api.commands.get(cmd.toLowerCase()).command(cmd, args.toArray(new String[args.size()]));
                    } catch (Exception e) {
                        log.error.println(new InvocationTargetException(e, "Uncaught exception while running command"));
                    }
                } else {
                    String s = cmd.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"").replace(" ", "\\ ");
                    for (String arg : args) {
                        s += ' ' + arg.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"").replace(" ", "\\ ");
                    }
                    log.message.println("Unknown Command - " + s);
                }
                jline.getOutput().write("\b \b");
            }
        }
    }
    /**
     * Parse escapes in a command
     *
     * @param str String
     * @return Unescaped String
     */
    private String unescapeCommand(String str) {
        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == str.length() - 1) ? '\\' : str
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                            && str.charAt(i + 1) <= '7') {
                        code += str.charAt(i + 1);
                        i++;
                        if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                                && str.charAt(i + 1) <= '7') {
                            code += str.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case ' ':
                        ch = ' ';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= str.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + str.charAt(i + 2) + str.charAt(i + 3)
                                        + str.charAt(i + 4) + str.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
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

            if (new File(dir, "Templates").exists()) Util.deleteDirectory(new File(dir, "Templates"));

            Util.isException(FileLogger::end);
            System.exit(exit);
        }
    }
}