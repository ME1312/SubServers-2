package net.ME1312.SubServers.Host;

import com.dosse.upnp.UPnP;
import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Library.Version.VersionType;
import net.ME1312.Galaxi.Plugin.Plugin;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubServers.Host.Event.SubReloadEvent;
import net.ME1312.SubServers.Host.Executable.SubCreator;
import net.ME1312.SubServers.Host.Executable.SubServer;
import net.ME1312.SubServers.Host.Library.*;
import net.ME1312.SubServers.Host.Network.Cipher;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;

/**
 * SubServers.Host Main Class
 */
@Plugin(name = "SubServers.Host", version = "2.13.2b", authors = "ME1312", description = "Host SubServers from other Machines", website = "https://github.com/ME1312/SubServers-2")
public final class ExHost {
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public HashMap<String, SubCreator.ServerTemplate> templates = new HashMap<String, SubCreator.ServerTemplate>();
    public HashMap<String, SubServer> servers = new HashMap<String, SubServer>();
    public SubCreator creator;

    public Logger log;
    public PluginInfo info;
    public GalaxiEngine engine;
    public YAMLConfig config;
    public YAMLSection host = null;
    public SubDataClient subdata = null;

    public final SubAPI api = new SubAPI(this);

    private boolean running = false;

    /**
     * SubServers.Host Launch
     *
     * @param args Args
     * @throws Exception
     */
    static void main(String[] args) throws Exception {
        if (System.getProperty("RM.subservers", "true").equalsIgnoreCase("true")) {
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

                Version galaxi = Version.fromString(GalaxiEngine.class.getAnnotation(Plugin.class).version());
                Version subservers = Version.fromString(ExHost.class.getAnnotation(Plugin.class).version());
                Version galaxibuild = null;
                Version subserversbuild = null;
                try {
                    Manifest manifest = new Manifest(GalaxiEngine.class.getResourceAsStream("/META-INF/GalaxiEngine.MF"));
                    if (manifest.getMainAttributes().getValue("Implementation-Version") != null && manifest.getMainAttributes().getValue("Implementation-Version").length() > 0)
                        galaxibuild = new Version(manifest.getMainAttributes().getValue("Implementation-Version"));
                } catch (Exception e) {} try {
                    Field f = Version.class.getDeclaredField("type");
                    f.setAccessible(true);
                    if (f.get(subservers) != VersionType.SNAPSHOT && ExHost.class.getPackage().getSpecificationTitle() != null)
                        subserversbuild = new Version(ExHost.class.getPackage().getSpecificationTitle());
                    f.setAccessible(false);
                } catch (Exception e) {}

                System.out.println("");
                System.out.println(System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                System.out.println("Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                System.out.println(GalaxiEngine.class.getAnnotation(Plugin.class).name() + " v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"")
                        + ((GalaxiEngine.class.getProtectionDomain().getCodeSource().getLocation().equals(ExHost.class.getProtectionDomain().getCodeSource().getLocation()))?" [Patched]":"") + ',');
                System.out.println(ExHost.class.getAnnotation(Plugin.class).name() + " v" + subservers.toExtendedString() + ((subserversbuild != null)?" (" + subserversbuild + ')':""));
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
        log = new Logger("SubServers");

        try {
            info = PluginInfo.getPluginInfo(this);
            info.setLogger(log);
            if (ExHost.class.getPackage().getSpecificationTitle() != null) info.setSignature(new Version(ExHost.class.getPackage().getSpecificationTitle()));
            info.setIcon(ExHost.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/icon.png"));
            engine = GalaxiEngine.init(info);
            log.info.println("Loading SubServers.Host v" + info.getVersion().toString() + " Libraries");
            if (!(new UniversalFile(engine.getRuntimeDirectory(), "config.yml").exists())) {
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(engine.getRuntimeDirectory(), "config.yml").getPath());
                log.info.println("Created ~/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(engine.getRuntimeDirectory(), "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(engine.getRuntimeDirectory(), "config.yml").toPath(), new UniversalFile(engine.getRuntimeDirectory(), "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/config.yml", new UniversalFile(engine.getRuntimeDirectory(), "config.yml").getPath());
                log.info.println("Updated ~/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(engine.getRuntimeDirectory(), "config.yml"));

            if (!(new UniversalFile(engine.getRuntimeDirectory(), "Templates").exists())) {
                new UniversalFile(engine.getRuntimeDirectory(), "Templates").mkdir();
                log.info.println("Created ~/Templates/");
            }

            if (new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted").exists()) {
                int f = new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted").listFiles().length;
                for (File file : new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted").listFiles()) {
                    try {
                        if (file.isDirectory()) {
                            if (new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted:" + file.getName() + ":info.json").exists()) {
                                FileReader reader = new FileReader(new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted:" + file.getName() + ":info.json"));
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
                    Files.delete(new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted").toPath());
                }
            }

            engine.getPluginManager().loadPlugins(new UniversalFile(engine.getRuntimeDirectory(), "Plugins"));

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
                    engine.stop();
                }
            }));
            creator = new SubCreator(this);

            loadDefaults();

            new Metrics(this);
            info.setUpdateChecker(() -> {
                try {
                    YAMLSection tags = new YAMLSection(new JSONObject("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = info.getVersion();
                    int updcount = 0;
                    for (YAMLSection tag : tags.getSectionList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                    Collections.sort(versions);
                    for (Version version : versions) {
                        if (version.compareTo(updversion) > 0) {
                            updversion = version;
                            updcount++;
                        }
                    }
                    if (updcount > 0) log.info.println("SubServers.Host v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            });

            engine.start(this::stop);

            if (!UPnP.isUPnPAvailable()) {
                log.warn.println("UPnP is currently unavailable; Ports may not be automatically forwarded on this device");
            }
        } catch (Exception e) {
            log.error.println(e);
            engine.stop();
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

        engine.getPluginManager().executeEvent(new SubReloadEvent(this));
    }

    private void loadDefaults() {
        SubCommand.load(this);
    }

    private void stop() {
        if (running) {
            log.info.println("Shutting down...");

            List<String> subservers = new ArrayList<String>();
            subservers.addAll(servers.keySet());

            for (String server : subservers) {
                servers.get(server).stop();
                try {
                    servers.get(server).waitFor();
                } catch (Exception e) {
                    log.error.println(e);
                }
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(servers.get(server).getPort())) UPnP.closePortTCP(servers.get(server).getPort());
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

            if (new File(engine.getRuntimeDirectory(), "Templates").exists()) Util.deleteDirectory(new File(engine.getRuntimeDirectory(), "Templates"));
        }
    }
}