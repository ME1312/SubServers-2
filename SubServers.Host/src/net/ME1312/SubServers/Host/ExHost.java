package net.ME1312.SubServers.Host;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Event.Engine.GalaxiReloadEvent;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Library.Version.VersionType;
import net.ME1312.Galaxi.Log.Logger;
import net.ME1312.Galaxi.Plugin.App;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.DHE;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Executable.SubCreatorImpl;
import net.ME1312.SubServers.Host.Executable.SubLoggerImpl;
import net.ME1312.SubServers.Host.Executable.SubServerImpl;
import net.ME1312.SubServers.Host.Library.ConfigUpdater;
import net.ME1312.SubServers.Host.Library.Metrics;
import net.ME1312.SubServers.Host.Network.SubProtocol;

import com.dosse.upnp.UPnP;
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
@App(name = "SubServers.Host", version = "2.20a", authors = "ME1312", website = "https://github.com/ME1312/SubServers-2", description = "Host subservers on separate machines")
public final class ExHost {
    HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    Pair<Long, Map<String, Map<String, String>>> lang = null;
    public HashMap<String, SubCreatorImpl.ServerTemplate> templatesR = new HashMap<String, SubCreatorImpl.ServerTemplate>();
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
            joptsimple.OptionSet options = parser.parse(args);
            if (options.has("version") || options.has("v")) {
                Class<?> GalaxiEngine = Class.forName("net.ME1312.Galaxi.Engine.Runtime.Engine");
                Version galaxi = Version.fromString(GalaxiEngine.getAnnotation(App.class).version());
                Version subservers = Version.fromString(ExHost.class.getAnnotation(App.class).version());
                Version galaxibuild = null;
                Version subserversbuild = null;
                try {
                    Manifest manifest = new Manifest(GalaxiEngine.getResourceAsStream("/META-INF/GalaxiEngine.MF"));
                    if (manifest.getMainAttributes().getValue("Implementation-Version") != null && manifest.getMainAttributes().getValue("Implementation-Version").length() > 0)
                        galaxibuild = new Version(manifest.getMainAttributes().getValue("Implementation-Version"));
                } catch (Exception e) {} try {
                    if (Util.reflect(Version.class.getDeclaredField("type"), subservers) != VersionType.SNAPSHOT && ExHost.class.getPackage().getSpecificationTitle() != null)
                        subserversbuild = new Version(ExHost.class.getPackage().getSpecificationTitle());
                } catch (Exception e) {}

                System.out.println("");
                System.out.println(Platform.getSystemName() + ' ' + Platform.getSystemVersion() + ((Platform.getSystemBuild() != null)?" (" + Platform.getSystemBuild() + ')':"") + ((!Platform.getSystemArchitecture().equals("unknown"))?" [" + Platform.getSystemArchitecture() + ']':"") + ',');
                System.out.println("Java " + Platform.getJavaVersion() + ((!Platform.getJavaArchitecture().equals("unknown"))?" [" + Platform.getJavaArchitecture() + ']':"") + ',');
                System.out.println(GalaxiEngine.getAnnotation(App.class).name() + " v" + galaxi.toExtendedString() + ((galaxibuild != null)?" (" + galaxibuild + ')':"") + ',');
                System.out.println(ExHost.class.getAnnotation(App.class).name() + " v" + subservers.toExtendedString() + ((subserversbuild != null)?" (" + subserversbuild + ')':""));
                System.out.println("");
            } else {
                new ExHost(options);
            }
        } else {
            System.out.println(">> SubServers' hosting capabilities have been disallowed on this machine");
            System.out.println(">> Check with your provider for more information");
            System.exit(1);
        }
    }

    private ExHost(joptsimple.OptionSet options) {
        log = new Logger("SubServers");

        try {
            info = PluginInfo.load(this);
            info.setLogger(log);
            if (ExHost.class.getPackage().getSpecificationTitle() != null) info.setBuild(new Version(ExHost.class.getPackage().getSpecificationTitle()));
            info.setIcon(ExHost.class.getResourceAsStream("/net/ME1312/SubServers/Host/Library/Files/icon.png"));
            engine = GalaxiEngine.init(info);
            log.info.println("Loading SubServers.Host v" + info.getVersion().toString() + " Libraries");

            ConfigUpdater.updateConfig(new File(engine.getRuntimeDirectory(), "config.yml"));
            config = new YAMLConfig(new File(engine.getRuntimeDirectory(), "config.yml"));

            if (!(new File(engine.getRuntimeDirectory(), "Templates").exists())) {
                new File(engine.getRuntimeDirectory(), "Templates").mkdirs();
                log.info.println("Created ./Templates/");
            }

            if (new File(engine.getRuntimeDirectory(), "Recently Deleted").exists()) {
                int f = new File(engine.getRuntimeDirectory(), "Recently Deleted").listFiles().length;
                for (File file : new File(engine.getRuntimeDirectory(), "Recently Deleted").listFiles()) {
                    try {
                        if (file.isDirectory()) {
                            if (new File(engine.getRuntimeDirectory(), "Recently Deleted/" + file.getName() + "/info.json").exists()) {
                                FileReader reader = new FileReader(new File(engine.getRuntimeDirectory(), "Recently Deleted/" + file.getName() + "/info.json"));
                                JSONObject json = new JSONObject(Util.readAll(reader));
                                reader.close();
                                if (json.keySet().contains("Timestamp")) {
                                    if (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTime().getTime() - json.getLong("Timestamp")) >= 7) {
                                        f = removeFile(file, f, true);
                                    }
                                } else {
                                    f = removeFile(file, f, true);
                                }
                            } else {
                                f = removeFile(file, f, true);
                            }
                        } else {
                            f = removeFile(file, f, false);
                        }
                    } catch (Exception e) {
                        log.error.println(e);
                    }
                }
                if (f <= 0) {
                    Files.delete(new File(engine.getRuntimeDirectory(), "Recently Deleted").toPath());
                }
            }

            Util.reflect(SubLoggerImpl.class.getDeclaredField("logn"), null, config.get().getMap("Settings").getBoolean("Network-Log", true));
            Util.reflect(SubLoggerImpl.class.getDeclaredField("logc"), null, config.get().getMap("Settings").getBoolean("Console-Log", true));

            loadDefaults();
            engine.getPluginManager().loadPlugins(new File(engine.getRuntimeDirectory(), "Plugins"));

            running = true;
            creator = new SubCreatorImpl(this);
            reload(false);

            subdata.put(0, null);
            subprotocol = SubProtocol.get();
            subprotocol.registerCipher("DHE", DHE.get(128));
            subprotocol.registerCipher("DHE-128", DHE.get(128));
            subprotocol.registerCipher("DHE-192", DHE.get(192));
            subprotocol.registerCipher("DHE-256", DHE.get(256));
            api.name = config.get().getMap("Settings").getMap("SubData").getString("Name");
            Logger log = new Logger("SubData");

            if (config.get().getMap("Settings").getMap("SubData").getString("Password", "").length() > 0) {
                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getString("Password")));

                log.info.println("AES Encryption Available");
            }
            if (new File(engine.getRuntimeDirectory(), "subdata.rsa.key").exists()) {
                try {
                    subprotocol.registerCipher("RSA", new RSA(new File(engine.getRuntimeDirectory(), "subdata.rsa.key")));
                    log.info.println("RSA Encryption Available");
                } catch (Exception e) {
                    log.error.println(e);
                }
            }

            reconnect = true;
            log.info.println();
            log.info.println("Connecting to /" + config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391"));
            connect(log.toPrimitive(), null);

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
                    if (updcount > 0) {
                        log.info.println("SubServers.Host v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                        return true;
                    }
                } catch (Exception e) {}
                return false;
            });

            engine.start(this::stop);

            if (!UPnP.isUPnPAvailable()) {
                log.warn.println("UPnP service is unavailable. SubServers can't port-forward for you from this device.");
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

    /**
     * Removes a file from the Recently Deleted folder and decrements the 
     * fileLength
     * 
     * @param file The file to remove
     * @param fileLength The current file length
     * @param isDir Whether or not the file is a directory\
     * @return The new file length
     */
    private int removeFile(File file, int fileLength, boolean isDir) {
        if(isDir) { 
            Directories.delete(file);
        } else {
            Files.delete(file.toPath());
        }
        fileLength--;
        log.info.println("Removed ./Recently Deleted/" + file.getName());
        return fileLength;
    }
    
    private void loadDefaults() {
        new SubCommand(this).load();
    }

    public void reload(boolean notifyPlugins) throws IOException {
        resetDate = Calendar.getInstance().getTime().getTime();

        ConfigUpdater.updateConfig(new File(engine.getRuntimeDirectory(), "config.yml"));
        config.reload();
        creator.load(false);

        if (notifyPlugins) {
            engine.getPluginManager().executeEvent(new GalaxiReloadEvent(engine));
        }
    }

    private void connect(java.util.logging.Logger log, Pair<DisconnectReason, DataClient> disconnect) throws IOException {
        int reconnect = config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
        if (disconnect == null || (this.reconnect && reconnect > 0 && disconnect.key() != DisconnectReason.PROTOCOL_MISMATCH && disconnect.key() != DisconnectReason.ENCRYPTION_MISMATCH)) {
            long reset = resetDate;
            Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::SubData_Reconnect_Handler");
            if (disconnect != null) log.info("Attempting reconnect in " + reconnect + " seconds");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (reset == resetDate && (subdata.getOrDefault(0, null) == null || subdata.get(0).isClosed())) {
                            SubDataClient open = subprotocol.open(InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]));

                            if (subdata.getOrDefault(0, null) != null) subdata.get(0).reconnect(open);
                            subdata.put(0, open);
                        }
                        timer.cancel();
                    } catch (IOException e) {
                        log.info("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                    }
                }
            }, (disconnect == null)?0:TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
        }
    }

    @SuppressWarnings("unchecked")
    private void stop() {
        if (running) {
            log.info.println("Stopping hosted servers");
            Map.Entry<String, SubServerImpl>[] subservers = servers.entrySet().toArray(new Map.Entry[0]);

            for (Map.Entry<String, SubServerImpl> entry : subservers) {
                if (entry.getValue().isRunning()) {
                    log.info.println("Stopping " + entry.getValue().getName());
                    entry.getValue().stop();
                    try {
                        entry.getValue().waitFor();
                    } catch (Exception e) {
                        log.error.println(e);
                    }
                }
                servers.remove(entry.getKey());
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(entry.getValue().getPort())) UPnP.closePortTCP(entry.getValue().getPort());
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

            if (new File(engine.getRuntimeDirectory(), "Cache/Remote").exists()) Directories.delete(new File(engine.getRuntimeDirectory(), "Cache/Remote"));
        }
    }
}