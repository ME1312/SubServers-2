package net.ME1312.SubServers.Host;

import com.dosse.upnp.UPnP;
import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Event.GalaxiReloadEvent;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Library.Version.VersionType;
import net.ME1312.Galaxi.Plugin.App;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;
import net.ME1312.SubServers.Host.Executable.SubLoggerImpl;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.Library.*;
import net.ME1312.SubServers.Host.Library.Updates.ConfigUpdater;
import net.ME1312.SubServers.Host.Network.SubProtocol;
import org.json.JSONObject;

import java.io.*;
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
@App(name = "SubServers.Host", version = "2.14.4a", authors = "ME1312", website = "https://github.com/ME1312/SubServers-2", description = "Host subservers on separate machines")
public final class ExHost {
    HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public HashMap<String, SubCreatorImpl.ServerTemplate> templates = new HashMap<String, SubCreatorImpl.ServerTemplate>();
    public HashMap<String, SubServerImpl> servers = new HashMap<String, SubServerImpl>();
    public SubCreatorImpl creator;

    public Logger log;
    public PluginInfo info;
    public GalaxiEngine engine;
    public YAMLConfig config;
    public ObjectMap<String> host = null;
    public SubProtocol subprotocol;

    public final SubAPI api = new SubAPI(this);

    private long resetDate = 0;
    private boolean reconnect = true;
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

                Version galaxi = Version.fromString(GalaxiEngine.class.getAnnotation(App.class).version());
                Version subservers = Version.fromString(ExHost.class.getAnnotation(App.class).version());
                Version galaxibuild = null;
                Version subserversbuild = null;
                try {
                    Manifest manifest = new Manifest(GalaxiEngine.class.getResourceAsStream("/META-INF/GalaxiEngine.MF"));
                    if (manifest.getMainAttributes().getValue("Implementation-Version") != null && manifest.getMainAttributes().getValue("Implementation-Version").length() > 0)
                        galaxibuild = new Version(manifest.getMainAttributes().getValue("Implementation-Version"));
                } catch (Exception e) {} try {
                    if (Util.reflect(Version.class.getDeclaredField("type"), subservers) != VersionType.SNAPSHOT && ExHost.class.getPackage().getSpecificationTitle() != null)
                        subserversbuild = new Version(ExHost.class.getPackage().getSpecificationTitle());
                } catch (Exception e) {}

                System.out.println("");
                System.out.println(System.getProperty("os.name") + ((!System.getProperty("os.name").toLowerCase().startsWith("windows"))?' ' + System.getProperty("os.version"):"") + ((osarch != null)?" [" + osarch + ']':"") + ',');
                System.out.println("Java " + System.getProperty("java.version") + ((javaarch != null)?" [" + javaarch + ']':"") + ',');
                System.out.println(GalaxiEngine.class.getAnnotation(App.class).name() + " v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"") + ',');
                System.out.println(ExHost.class.getAnnotation(App.class).name() + " v" + subservers.toExtendedString() + ((subserversbuild != null)?" (" + subserversbuild + ')':""));
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

            ConfigUpdater.updateConfig(new UniversalFile(engine.getRuntimeDirectory(), "config.yml"));
            config = new YAMLConfig(new UniversalFile(engine.getRuntimeDirectory(), "config.yml"));

            if (!(new UniversalFile(engine.getRuntimeDirectory(), "Templates").exists())) {
                new UniversalFile(engine.getRuntimeDirectory(), "Templates").mkdir();
                log.info.println("Created ./Templates/");
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
                                        log.info.println("Removed ./Recently Deleted/" + file.getName());
                                    }
                                } else {
                                    Util.deleteDirectory(file);
                                    f--;
                                    log.info.println("Removed ./Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                log.info.println("Removed ./Recently Deleted/" + file.getName());
                            }
                        } else {
                            Files.delete(file.toPath());
                            f--;
                            log.info.println("Removed ./Recently Deleted/" + file.getName());
                        }
                    } catch (Exception e) {
                        log.error.println(e);
                    }
                }
                if (f <= 0) {
                    Files.delete(new UniversalFile(engine.getRuntimeDirectory(), "Recently Deleted").toPath());
                }
            }

            Util.reflect(SubLoggerImpl.class.getDeclaredField("logn"), null, config.get().getMap("Settings").getBoolean("Network-Log", true));
            Util.reflect(SubLoggerImpl.class.getDeclaredField("logc"), null, config.get().getMap("Settings").getBoolean("Console-Log", true));

            engine.getPluginManager().loadPlugins(new UniversalFile(engine.getRuntimeDirectory(), "Plugins"));

            running = true;
            creator = new SubCreatorImpl(this);
            subprotocol = SubProtocol.get();
            loadDefaults();
            reload(false);

            new Metrics(this);
            info.setUpdateChecker(() -> {
                try {
                    YAMLSection tags = new YAMLSection(new JSONObject("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = info.getVersion();
                    int updcount = 0;
                    for (ObjectMap<String> tag : tags.getMapList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
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
            if (engine == null) {
                e.printStackTrace();
                System.exit(1);
            } else {
                log.error.println(e);
                engine.stop(1);
            }
        }
    }

    private void loadDefaults() {
        SubCommand.load(this);
    }

    public void reload(boolean notifyPlugins) throws IOException {
        resetDate = Calendar.getInstance().getTime().getTime();
        reconnect = false;
        ArrayList<SubDataClient> tmp = new ArrayList<SubDataClient>();
        tmp.addAll(subdata.values());
        for (SubDataClient client : tmp) if (client != null) {
            client.close();
            Util.isException(client::waitFor);
        }
        subdata.clear();
        subdata.put(0, null);

        ConfigUpdater.updateConfig(new UniversalFile(engine.getRuntimeDirectory(), "config.yml"));
        config.reload();

        subprotocol.unregisterCipher("AES");
        subprotocol.unregisterCipher("AES-128");
        subprotocol.unregisterCipher("AES-192");
        subprotocol.unregisterCipher("AES-256");
        subprotocol.unregisterCipher("RSA");
        api.name = config.get().getMap("Settings").getMap("SubData").getString("Name", null);
        Logger log = new Logger("SubData");

        if (config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) {
            subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));

            log.info.println("AES Encryption Available");
        }
        if (new UniversalFile(engine.getRuntimeDirectory(), "subdata.rsa.key").exists()) {
            try {
                subprotocol.registerCipher("RSA", new RSA(new UniversalFile(engine.getRuntimeDirectory(), "subdata.rsa.key")));
                log.info.println("RSA Encryption Available");
            } catch (Exception e) {
                log.error.println(e);
            }
        }

        reconnect = true;
        log.info.println();
        log.info.println("Connecting to /" + config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391"));
        connect(log.toPrimitive(), null);

        if (notifyPlugins) {
            engine.getPluginManager().executeEvent(new GalaxiReloadEvent(engine));
        }
    }

    private void connect(java.util.logging.Logger log, NamedContainer<DisconnectReason, DataClient> disconnect) throws IOException {
        int reconnect = config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 30);
        if (disconnect == null || (this.reconnect && reconnect > 0 && disconnect.name() != DisconnectReason.PROTOCOL_MISMATCH && disconnect.name() != DisconnectReason.ENCRYPTION_MISMATCH)) {
            long reset = resetDate;
            Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::SubData_Reconnect_Handler");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (reset == resetDate && subdata.getOrDefault(0, null) == null)
                            subdata.put(0, subprotocol.open((config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?
                                        null:InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1])));
                        timer.cancel();
                    } catch (IOException e) {
                        log.info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                    }
                }
            }, (disconnect == null)?0:TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
        }
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

            reconnect = false;
            try {
                ArrayList<SubDataClient> temp = new ArrayList<SubDataClient>();
                temp.addAll(subdata.values());
                for (SubDataClient client : temp) if (client != null)  {
                    client.close();
                    client.waitFor();
                }
                subdata.clear();
                subdata.put(0, null);
            } catch (Exception e) {
                log.error.println(e);
            }

            if (new File(engine.getRuntimeDirectory(), "Templates").exists()) Util.deleteDirectory(new File(engine.getRuntimeDirectory(), "Templates"));
        }
    }
}