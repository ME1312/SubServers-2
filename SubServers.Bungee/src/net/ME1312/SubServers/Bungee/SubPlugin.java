package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Event.SubStoppedEvent;
import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.Metrics;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 */
public final class SubPlugin extends BungeeCord implements Listener {
    protected final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();
    public final HashMap<String, List<Server>> groups = new HashMap<String, List<Server>>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    private final HashMap<String, ServerInfo> legServers = new HashMap<String, ServerInfo>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLConfig lang;
    public HashMap<String, String> exLang = new HashMap<String, String>();
    public SubDataServer subdata = null;
    public SubServer sudo = null;
    public final Version version = new Version(SubPlugin.class.getPackage().getImplementationVersion());
    public final Version bversion = (SubPlugin.class.getPackage().getSpecificationVersion().equals("0"))?null:new Version(SubPlugin.class.getPackage().getSpecificationVersion());

    private boolean running = false;
    private boolean posted = false;
    public final SubAPI api = new SubAPI(this);

    protected SubPlugin(PrintStream out) throws IOException {
        System.out.println("SubServers > Loading SubServers.Bungee v" + version.toString() + " Libraries... ");

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            System.out.println("SubServers > Created ~/config.yml");
        }
        if (!(new UniversalFile(dir, "modules.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/modules.yml", new UniversalFile(dir, "modules.yml").getPath());
            System.out.println("SubServers > Created ~/modules.yml");
        }
        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/config.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
            Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/config.yml");
        }
        config = new YAMLConfig(new UniversalFile(dir, "config.yml"));

        if (!(new UniversalFile(dir, "lang.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/lang.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "lang.yml"))).get().getString("Version", "0")).compareTo(new Version("2.12.1e+"))) != 0) {
            Files.move(new UniversalFile(dir, "lang.yml").toPath(), new UniversalFile(dir, "lang.old" + Math.round(Math.random() * 100000) + ".yml").toPath());
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/lang.yml");
        }
        lang = new YAMLConfig(new UniversalFile(dir, "lang.yml"));

        if (!(new UniversalFile(dir, "Templates").exists())) {
            new UniversalFile(dir, "Templates").mkdirs();

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Vanilla");

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Spigot");

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Sponge");
        } else {
            if (new UniversalFile(dir, "Templates:Vanilla:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Vanilla:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.12b+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Vanilla").toPath(), new UniversalFile(dir, "Templates:Vanilla.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Vanilla");
            }
            if (new UniversalFile(dir, "Templates:Spigot:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Spigot:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.11.2m+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Vanilla").toPath(), new UniversalFile(dir, "Templates:Spigot.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Spigot");
            }
            if (new UniversalFile(dir, "Templates:Sponge:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Sponge:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.11.2m+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Vanilla").toPath(), new UniversalFile(dir, "Templates:Sponge.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Sponge");
            }
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
                                    System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                            }
                        } else {
                            Util.deleteDirectory(file);
                            f--;
                            System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                        }
                    } else {
                        Files.delete(file.toPath());
                        f--;
                        System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                    }
                } catch (Exception e) {
                    System.out.println("SubServers > Problem scanning ~/SubServers/Recently Deleted/" + file.getName());
                    e.printStackTrace();
                    Files.delete(file.toPath());
                }
            }
            if (f <= 0) {
                Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
            }
        }

        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.Internal.InternalHost.class, "built-in");
        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.External.ExternalHost.class, "network");

        getPluginManager().registerListener(null, this);
        getPluginManager().registerCommand(null, new SubCommand.BungeeServer(this, "server"));
        getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
        getPluginManager().registerCommand(null, new SubCommand(this, "subservers"));
        getPluginManager().registerCommand(null, new SubCommand(this, "subserver"));
        getPluginManager().registerCommand(null, new SubCommand(this, "sub"));

        System.out.println("SubServers > Pre-Parsing Config...");
        for (String name : config.get().getSection("Servers").getKeys()) {
            try {
                if (!config.get().getSection("Hosts").contains(config.get().getSection("Servers").getSection(name).getString("Host"))) throw new InvalidServerException("There is no host with this name: " + config.get().getSection("Servers").getSection(name).getString("Host"));
                legServers.put(name, new BungeeServerInfo(name, new InetSocketAddress(InetAddress.getByName(config.get().getSection("Hosts").getSection(config.get().getSection("Servers").getSection(name).getString("Host")).getRawString("Address")), config.get().getSection("Servers").getSection(name).getInt("Port")), config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&'), config.get().getSection("Servers").getSection(name).getBoolean("Restricted")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("SubServers > Loading BungeeCord Libraries...");
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData Direct
     */
    @Override
    public void startListeners() {
        try {
            long begin = Calendar.getInstance().getTime().getTime();

            config.reload();
            lang.reload();
            SubDataServer.Encryption encryption = SubDataServer.Encryption.NONE;
            if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                System.out.println("SubData > Cannot encrypt connection without a password");
            } else if (Util.isException(() -> SubDataServer.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase()))) {
                System.out.println("SubData > Unknown encryption type: " + SubDataServer.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "None")));
            } else {
                encryption = SubDataServer.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase());
            }
            subdata = new SubDataServer(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]),
                    (config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                    encryption);
            System.out.println("SubServers > SubData Direct Listening on " + subdata.getServer().getLocalSocketAddress().toString());
            loop();

            int hosts = 0;
            System.out.println("SubServers > Loading Hosts...");
            for (String name : config.get().getSection("Hosts").getKeys()) {
                try {
                    if (!hostDrivers.keySet().contains(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toLowerCase())) throw new InvalidHostException("Invalid Driver for host: " + name);
                    Host host = api.addHost(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toLowerCase(), name, config.get().getSection("Hosts").getSection(name).getBoolean("Enabled"), InetAddress.getByName(config.get().getSection("Hosts").getSection(name).getRawString("Address")),
                            config.get().getSection("Hosts").getSection(name).getRawString("Directory"), config.get().getSection("Hosts").getSection(name).getRawString("Git-Bash"));
                    if (config.get().getSection("Hosts").getSection(name).getKeys().contains("Display") && config.get().getSection("Hosts").getSection(name).getString("Display").length() > 0) host.setDisplayName(config.get().getSection("Hosts").getSection(name).getString("Display"));
                    if (config.get().getSection("Hosts").getSection(name).getKeys().contains("Extra")) for (String extra : config.get().getSection("Hosts").getSection(name).getSection("Extra").getKeys()) host.addExtra(extra, config.get().getSection("Hosts").getSection(name).getSection("Extra").getObject(extra));
                    hosts++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int servers = 0;
            System.out.println("SubServers > Loading Servers...");
            YAMLConfig bungee = new YAMLConfig(new UniversalFile(dir, "config.yml"));
            for (String name : bungee.get().getSection("servers").getKeys()) {
                try {
                    Server server = api.addServer(name, InetAddress.getByName(bungee.get().getSection("servers").getSection(name).getRawString("address").split(":")[0]),
                            Integer.parseInt(bungee.get().getSection("servers").getSection(name).getRawString("address").split(":")[1]), bungee.get().getSection("servers").getSection(name).getColoredString("motd", '&'),
                            bungee.get().getSection("servers").getSection(name).getBoolean("hidden", false), bungee.get().getSection("servers").getSection(name).getBoolean("restricted"));
                    if (bungee.get().getSection("servers").getSection(name).getKeys().contains("display") && bungee.get().getSection("servers").getSection(name).getString("display").length() > 0) server.setDisplayName(bungee.get().getSection("servers").getSection(name).getString("display"));
                    if (bungee.get().getSection("servers").getSection(name).getKeys().contains("group")) for (String group : bungee.get().getSection("servers").getSection(name).getStringList("group")) server.addGroup(group);
                    if (bungee.get().getSection("servers").getSection(name).getKeys().contains("extra")) for (String extra : config.get().getSection("servers").getSection(name).getSection("extra").getKeys()) server.addExtra(extra, config.get().getSection("servers").getSection(name).getSection("extra").getObject(extra));
                    servers++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int subservers = 0;
            System.out.println("SubServers > Loading SubServers...");
            for (String name : config.get().getSection("Servers").getKeys()) {
                try {
                    if (!this.hosts.keySet().contains(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase())) throw new InvalidServerException("There is no host with this name: " + config.get().getSection("Servers").getSection(name).getString("Host"));
                    if (exServers.keySet().contains(name.toLowerCase())) {
                        exServers.remove(name.toLowerCase());
                        servers--;
                    }
                    SubServer server = this.hosts.get(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase()).addSubServer(name, config.get().getSection("Servers").getSection(name).getBoolean("Enabled"),
                            config.get().getSection("Servers").getSection(name).getInt("Port"), config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&'), config.get().getSection("Servers").getSection(name).getBoolean("Log"),
                            config.get().getSection("Servers").getSection(name).getRawString("Directory"), new Executable(config.get().getSection("Servers").getSection(name).getRawString("Executable")), config.get().getSection("Servers").getSection(name).getRawString("Stop-Command"),
                            config.get().getSection("Servers").getSection(name).getBoolean("Run-On-Launch"), config.get().getSection("Servers").getSection(name).getBoolean("Auto-Restart"), config.get().getSection("Servers").getSection(name).getBoolean("Hidden"), config.get().getSection("Servers").getSection(name).getBoolean("Restricted"), false);
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Display") && config.get().getSection("Servers").getSection(name).getString("Display").length() > 0) server.setDisplayName(config.get().getSection("Servers").getSection(name).getString("Display"));
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Group")) for (String group : config.get().getSection("Servers").getSection(name).getStringList("Group")) server.addGroup(group);
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Extra")) for (String extra : config.get().getSection("Servers").getSection(name).getSection("Extra").getKeys()) server.addExtra(extra, config.get().getSection("Servers").getSection(name).getSection("Extra").getObject(extra));
                    subservers++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (SubServer server : api.getSubServers().values()) {
                for (String name : config.get().getSection("Servers").getSection(server.getName()).getRawStringList("Incompatible", new ArrayList<>())) {
                    SubServer other = api.getSubServer(name);
                    if (other != null && server.isCompatible(other)) server.toggleCompatibility(other);
                }
            }
            running = true;
            legServers.clear();

            int plugins = 0;
            if (api.listeners.size() > 0) {
                System.out.println("SubServers > Loading SubAPI Plugins...");
                for (NamedContainer<Runnable, Runnable> listener : api.listeners) {
                    try {
                        if (listener.name() != null) {
                            listener.name().run();
                            plugins++;
                        }
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem enabling plugin").printStackTrace();
                    }
                }
            }

            System.out.println("SubServers > " + ((plugins > 0)?plugins+" Plugin"+((plugins == 1)?"":"s")+", ":"") + hosts + " Host"+((hosts == 1)?"":"s")+", " + servers + " Server"+((servers == 1)?"":"s")+", and " + subservers + " SubServer"+((subservers == 1)?"":"s")+" loaded in " + new DecimalFormat("0.000").format((Calendar.getInstance().getTime().getTime() - begin) / 1000D) + "s");

            super.startListeners();
            if (!posted) {
                post();
                posted = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void post() {
        new Metrics(this);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Bungee/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

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
                    if (!updversion.equals(version)) System.out.println("SubServers > SubServers.Bungee v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));
    }

    private void loop() {
        new Thread(() -> {
            while (subdata != null) {
                try {
                    subdata.addClient(subdata.getServer().accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Emulate BungeeCord's getServers()
     *
     * @see SubAPI#getServers()
     * @return Server Map
     */
    @Override
    public Map<String, ServerInfo> getServers() {
        HashMap<String, ServerInfo> servers = new HashMap<String, ServerInfo>();
        if (!running) {
            servers.putAll(super.getServers());
            servers.putAll(legServers);
        } else {
            for (ServerInfo server : exServers.values()) servers.put(server.getName(), server);
            for (Host host : this.hosts.values()) {
                for (ServerInfo server : host.getSubServers().values()) servers.put(server.getName(), server);
            }
        }
        return servers;
    }

    /**
     * Reset all changes made by startListeners
     *
     * @see SubPlugin#startListeners()
     */
    @Override
    public void stopListeners() {
        try {
            legServers.clear();
            legServers.putAll(getServers());
            if (api.listeners.size() > 0) {
                System.out.println("SubServers > Resetting SubAPI Plugins...");
                for (NamedContainer<Runnable, Runnable> listener : api.listeners) {
                    try {
                        if (listener.get() != null) listener.get().run();
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem disabling plugin").printStackTrace();
                    }
                }
            }
            running = false;
            System.out.println("SubServers > Resetting Hosts and Server Data");
            List<String> hosts = new ArrayList<String>();
            hosts.addAll(this.hosts.keySet());

            for (String host : hosts) {
                List<String> subservers = new ArrayList<String>();
                subservers.addAll(this.hosts.get(host).getSubServers().keySet());

                for (String server : subservers) {
                    this.hosts.get(host).removeSubServer(server);
                }
                subservers.clear();
                this.hosts.get(host).getCreator().terminate();
                this.hosts.get(host).getCreator().waitFor();
                this.hosts.remove(host);
            }
            hosts.clear();
            exServers.clear();

            subdata.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.stopListeners();
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void reroute(ServerConnectEvent e) {
        Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(api.getServers());
        if (servers.keySet().contains(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
            e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
        } else {
            servers = getServers();
            if (servers.keySet().contains(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
                e.setTarget(servers.get(e.getTarget().getName()));
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void resetSudo(SubStoppedEvent e) {
        if (sudo == e.getServer()) {
            sudo = null;
            System.out.println("SubServers > Reverting to the BungeeCord Console");
        }
    }
}
