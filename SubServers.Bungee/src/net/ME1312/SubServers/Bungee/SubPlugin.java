package net.ME1312.SubServers.Bungee;

import net.ME1312.SubServers.Bungee.Host.Executable;
import net.ME1312.SubServers.Bungee.Host.Server;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 *
 * @author ME1312
 */
public final class SubPlugin extends BungeeCord {
    protected final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();

    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public YAMLConfig lang;
    public HashMap<String, String> exLang = new HashMap<String, String>();
    public SubDataServer subdata = null;
    public final Version version = new Version("2.11.2c");
    protected Version bversion = null;

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

        if (!(new UniversalFile(dir, "lang.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/lang.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "lang.yml"))).get().getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
            Files.move(new UniversalFile(dir, "lang.yml").toPath(), new UniversalFile(dir, "lang.old" + Math.round(Math.random() * 100000) + ".yml").toPath());
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/lang.yml");
        }

        if (!(new UniversalFile(dir, "build.sh").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/build.sh", new UniversalFile(dir, "build.sh").getPath());
            System.out.println("SubServers > Created ~/SubServers/build.sh");
        } else {
            String Version = "null";
            BufferedReader brText = new BufferedReader(new FileReader(new UniversalFile(dir, "build.sh")));
            try {
                Version = brText.readLine().split("Version: ")[1];
            } catch (NullPointerException e) {}
            brText.close();

            if (!Version.equalsIgnoreCase("2.11.2b+")) {
                Files.move(new UniversalFile(dir, "build.sh").toPath(), new UniversalFile(dir, "build.old" + Math.round(Math.random() * 100000) + ".sh").toPath());
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/build.sh", new UniversalFile(dir, "build.sh").getPath());
                System.out.println("SubServers > Updated ~/SubServers/build.sh");
            }
        }

        if (!(new UniversalFile(dir, "Plugin Templates:Spigot Plugins").exists())) {
            new UniversalFile(dir, "Plugin Templates:Spigot Plugins").mkdirs();
            System.out.println("SubServers > Created ~/SubServers/Plugin Templates/Spigot Plugins");
        }
        if (!(new UniversalFile(dir, "Plugin Templates:Sponge Config").exists())) {
            new UniversalFile(dir, "Plugin Templates:Sponge Config").mkdir();
            System.out.println("SubServers > Created ~/SubServers/Plugin Templates/Sponge Config");
        }
        if (!(new UniversalFile(dir, "Plugin Templates:Sponge Mods").exists())) {
            new UniversalFile(dir, "Plugin Templates:Sponge Mods").mkdir();
            System.out.println("SubServers > Created ~/SubServers/Plugin Templates/Sponge Mods");
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
                    e.printStackTrace();
                }
            }
            if (f == 0) {
                System.out.println("SubServers > Removed ~/SubServers/Recently Deleted");
                Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
            }
        }

        hostDrivers.put("built-in", net.ME1312.SubServers.Bungee.Host.Internal.InternalHost.class);

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
            subdata = new SubDataServer(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]), 10,
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]));
            System.out.println("SubServers > SubData Listening on " + subdata.getServer().getLocalSocketAddress().toString());
            loop();

            long begin = Calendar.getInstance().getTime().getTime();
            int hosts = 0;
            System.out.println("SubServers > Loading Hosts...");
            for (String name : config.get().getSection("Hosts").getKeys()) {
                try {
                    if (!hostDrivers.keySet().contains(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toLowerCase())) throw new InvalidHostException("Invalid Driver for host: " + name);
                    Host host = hostDrivers.get(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toLowerCase()).getConstructor(SubPlugin.class, String.class, Boolean.class, InetAddress.class, String.class, String.class).newInstance(
                            this, name, (Boolean) config.get().getSection("Hosts").getSection(name).getBoolean("Enabled"), InetAddress.getByName(config.get().getSection("Hosts").getSection(name).getRawString("Address")), config.get().getSection("Hosts").getSection(name).getRawString("Directory"),
                            config.get().getSection("Hosts").getSection(name).getRawString("Git-Bash"));
                    this.hosts.put(name.toLowerCase(), host);
                    SubDataServer.allowConnection(host.getAddress());
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
                    Server server = new Server(name, new InetSocketAddress(bungee.get().getSection("servers").getSection(name).getRawString("address").split(":")[0],
                            Integer.parseInt(bungee.get().getSection("servers").getSection(name).getRawString("address").split(":")[1])), bungee.get().getSection("servers").getSection(name).getColoredString("motd", '&'),
                            bungee.get().getSection("servers").getSection(name).getBoolean("hidden", false), bungee.get().getSection("servers").getSection(name).getBoolean("restricted"));
                    exServers.put(name.toLowerCase(), server);
                    SubDataServer.allowConnection(server.getAddress().getAddress());
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
                            config.get().getSection("Servers").getSection(name).getBoolean("Run-On-Launch"), config.get().getSection("Servers").getSection(name).getBoolean("Hidden"), config.get().getSection("Servers").getSection(name).getBoolean("Auto-Restart"), config.get().getSection("Servers").getSection(name).getBoolean("Restricted"), false);
                    subservers++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("SubServers > " + hosts + " Host(s), " + servers + " Server(s), and " + subservers + " SubServer(s) loaded in " + (Calendar.getInstance().getTime().getTime() - begin) + "ms");

            getPluginManager().registerCommand(null, new SubCommand.BungeeServer(this, "server"));
            getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
            getPluginManager().registerCommand(null, new SubCommand(this, "subservers"));
            getPluginManager().registerCommand(null, new SubCommand(this, "subserver"));
            getPluginManager().registerCommand(null, new SubCommand(this, "sub"));

            super.startListeners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loop() {
        new Thread(() -> {
            while (running && subdata != null) {
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
        for (ServerInfo server : exServers.values()) servers.put(server.getName(), server);
        for (Host host : this.hosts.values()) {
            for (ServerInfo server : host.getSubServers().values()) servers.put(server.getName(), server);
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
                if (this.hosts.get(host).getCreator().isBusy()) {
                    this.hosts.get(host).getCreator().terminate();
                    this.hosts.get(host).getCreator().waitFor();
                }
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
}
