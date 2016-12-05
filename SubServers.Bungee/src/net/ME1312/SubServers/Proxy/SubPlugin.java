package net.ME1312.SubServers.Proxy;

import net.ME1312.SubServers.Proxy.Host.Executable;
import net.ME1312.SubServers.Proxy.Host.Server;
import net.ME1312.SubServers.Proxy.Libraries.Config.YAMLConfig;
import net.ME1312.SubServers.Proxy.Libraries.Exception.InvalidHostException;
import net.ME1312.SubServers.Proxy.Libraries.Exception.InvalidServerException;
import net.ME1312.SubServers.Proxy.Host.Host;
import net.ME1312.SubServers.Proxy.Host.SubServer;
import net.ME1312.SubServers.Proxy.Libraries.UniversalFile;
import net.ME1312.SubServers.Proxy.Libraries.Version.Version;
import net.ME1312.SubServers.Proxy.Network.NetworkManager;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;

/**
 * Main Plugin Class
 *
 * @author ME1312
 */
public final class SubPlugin extends BungeeCord {
    protected final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();

    public final UniversalFile dir = new UniversalFile(new File("./"));
    public YAMLConfig config;
    public YAMLConfig lang;
    public NetworkManager subdata;
    public final Version version = new Version("2.11.0a");

    protected boolean running = false;
    public final SubAPI api = new SubAPI(this);

    protected SubPlugin() throws IOException {
        enable();
    }

    /**
     * Enable Plugin
     *
     * @throws IOException
     */
    protected void enable() throws IOException {
        if (running) throw new IllegalStateException("SubServers has already been loaded");
        System.out.println("SubServers > Loading SubServers v" + version.toString() + " Libraries... ");
        running = true;
        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/config.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.0a+"))) != 0) {
            Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

            copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/config.yml");
        }

        if (!(new UniversalFile(dir, "lang.yml").exists())) {
            copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/lang.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "lang.yml"))).get().getString("Version", "0")).compareTo(new Version("2.11.0a+"))) != 0) {
            Files.move(new UniversalFile(dir, "lang.yml").toPath(), new UniversalFile(dir, "lang.old" + Math.round(Math.random() * 100000) + ".yml").toPath());
            copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/lang.yml");
        }

        if (!(new UniversalFile(dir, "build.sh").exists())) {
            copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/build.sh", new UniversalFile(dir, "build.sh").getPath());
            System.out.println("SubServers > Created ~/SubServers/build.sh");
        } else {
            String Version = "null";
            BufferedReader brText = new BufferedReader(new FileReader(new UniversalFile(dir, "build.sh")));
            try {
                Version = brText.readLine().split("Version: ")[1];
            } catch (NullPointerException e) {}
            brText.close();

            if (!Version.equalsIgnoreCase("2.11.0a+")) {
                Files.move(new UniversalFile(dir, "build.sh").toPath(), new UniversalFile(dir, "build.old" + Math.round(Math.random() * 100000) + ".sh").toPath());
                copyFromJar("net/ME1312/SubServers/Proxy/Libraries/Files/build.sh", new UniversalFile(dir, "build.sh").getPath());
                System.out.println("SubServers > Updated ~/SubServers/build.sh");
            }
        }

        hostDrivers.put("built-in", net.ME1312.SubServers.Proxy.Host.Internal.InternalHost.class);

        System.out.println("SubServers > Loading BungeeCord Libraries...");
    }

    /**
     * Load Hosts, Servers, SubServers, and SubData.
     */
    @Override
    public void startListeners() {
        try {
            config = new YAMLConfig(new UniversalFile(dir, "SubServers:config.yml"));
            lang = new YAMLConfig(new UniversalFile(dir, "SubServers:lang.yml"));
            subdata = new NetworkManager(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), 10,
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]));
            System.out.println("SubServers > SubData Listening on " + subdata.getServer().getLocalSocketAddress().toString());
            loop();

            long begin = Calendar.getInstance().getTime().getTime();
            int hosts = 0;
            System.out.println("SubServers > Loading Hosts...");
            for (String name : config.get().getSection("Hosts").getKeys()) {
                try {
                    if (name.contains(" ")) throw new InvalidHostException("Host names cannot have spaces: " + name);
                    if (!hostDrivers.keySet().contains(config.get().getSection("Hosts").getSection(name).getString("Driver").toLowerCase())) throw new InvalidHostException("Invalid Driver for host: " + name);
                    Host host = hostDrivers.get(config.get().getSection("Hosts").getSection(name).getString("Driver").toLowerCase()).getConstructor(SubPlugin.class, String.class, Boolean.class, InetAddress.class, UniversalFile.class).newInstance(
                            this, name, (Boolean) config.get().getSection("Hosts").getSection(name).getBoolean("Enabled"), InetAddress.getByName(config.get().getSection("Hosts").getSection(name).getString("Address")), new UniversalFile(new File(config.get().getSection("Hosts").getSection(name).getString("Directory"))));
                    this.hosts.put(name.toLowerCase(), host);
                    subdata.allowConnection(host.getAddress());
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
                    Server server = new Server(name, new InetSocketAddress(bungee.get().getSection("servers").getSection(name).getString("address").split(":")[0],
                            Integer.parseInt(bungee.get().getSection("servers").getSection(name).getString("address").split(":")[1])), bungee.get().getSection("servers").getSection(name).getString("motd"),
                            bungee.get().getSection("servers").getSection(name).getBoolean("restricted"));
                    exServers.put(name.toLowerCase(), server);
                    subdata.allowConnection(server.getAddress().getAddress());
                    servers++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int subservers = 0;
            System.out.println("SubServers > Loading SubServers...");
            for (String name : config.get().getSection("Servers").getKeys()) {
                try {
                    if (!this.hosts.keySet().contains(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase())) throw new InvalidServerException("There is no host with this name:" + name);
                    SubServer server = this.hosts.get(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase()).addSubServer(name, config.get().getSection("Servers").getSection(name).getBoolean("Enabled"),
                            config.get().getSection("Servers").getSection(name).getInt("Port"), config.get().getSection("Servers").getSection(name).getString("Motd"), config.get().getSection("Servers").getSection(name).getBoolean("Log"),
                            config.get().getSection("Servers").getSection(name).getString("Directory"), new Executable(config.get().getSection("Servers").getSection(name).getString("Executable")), config.get().getSection("Servers").getSection(name).getString("Stop-Command"),
                            config.get().getSection("Servers").getSection(name).getBoolean("Run-On-Launch"), config.get().getSection("Servers").getSection(name).getBoolean("Auto-Restart"), false);

                    subservers++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("SubServers > " + hosts + " Host(s), " + servers + " Server(s), and " + subservers + " SubServer(s) loaded in " + (Calendar.getInstance().getTime().getTime() - begin) + "ms");

            getPluginManager().registerCommand(null, new SubCommand(this));

            super.startListeners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * SubData Listener Loop
     */
    public void loop() {
        new Thread() {
            public void run() {
                while(running && subdata != null) {
                    try {
                        subdata.addClient(subdata.getServer().accept());
                    } catch (IOException e) {
                        if (e.getMessage() == null || !e.getMessage().equals("Socket closed")) e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Override BungeeCord Servers
     *
     * @see SubAPI#getServers()
     * @return Server Map
     */
    @Override
    public Map<String, ServerInfo> getServers() {
        TreeMap<String, ServerInfo> servers = new TreeMap<String, ServerInfo>();
        servers.putAll(this.exServers);
        for (Host host : this.hosts.values()) {
            servers.putAll(host.getSubServers());
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

    /**
     * Disable Plugin
     */
    protected void disable() {
        if (running) {
            running = false;
        }
    }

    /**
     * Override BungeeCord Stop Functions
     */
    @Override
    public void stop() {
        disable();
        super.stop();
    }

    /**
     * Override BungeeCord Stop Functions
     *
     * @param reason Reason
     */
    @Override
    public void stop(String reason) {
        disable();
        super.stop(reason);
    }

    private void copyFromJar(String resource, String destination) {
        InputStream resStreamIn = SubPlugin.class.getClassLoader().getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
