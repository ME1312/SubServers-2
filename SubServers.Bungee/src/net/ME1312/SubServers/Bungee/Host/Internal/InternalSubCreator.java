package net.ME1312.SubServers.Bungee.Host.Internal;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import net.ME1312.Galaxi.Library.*;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Exception.SubCreatorException;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.ChatColor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Internal SubCreator Class
 */
@SuppressWarnings("unchecked")
public class InternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private InternalHost host;
    private Range<Integer> ports;
    private Container<Boolean> log;
    private String gitBash;
    private TreeMap<String, CreatorTask> thread;

    private class CreatorTask extends Thread {
        private final UUID player;
        private final String name;
        private final ServerTemplate template;
        private final Version version;
        private final int port;
        private final InternalSubLogger log;
        private final Callback<SubServer> callback;
        private Process process;

        private CreatorTask(UUID player, String name, ServerTemplate template, Version version, int port, Callback<SubServer> callback) {
            super("SubServers.Bungee::Internal_SubCreator_Process_Handler(" + name + ')');
            this.player = player;
            this.name = name;
            this.template = template;
            this.version = version;
            this.port = port;
            this.log = new InternalSubLogger(null, this, name + File.separator + "Creator", InternalSubCreator.this.log, null);
            this.callback = callback;
        }

        private ObjectMap<String> build(File dir, ServerTemplate template, List<ServerTemplate> history) throws SubCreatorException {
            ObjectMap<String> server = new ObjectMap<String>();
            Version version = this.version;
            HashMap<String, String> var = new HashMap<String, String>();
            boolean error = false;
            if (history.contains(template)) throw new IllegalStateException("Template Import loop detected");
            history.add(template);
            for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
                if (templates.keySet().contains(other.toLowerCase())) {
                    if (templates.get(other.toLowerCase()).isEnabled()) {
                        if (version != null || !templates.get(other.toLowerCase()).requiresVersion()) {
                            ObjectMap<String> config = build(dir, templates.get(other.toLowerCase()), history);
                            if (config == null) {
                                throw new SubCreatorException();
                            } else {
                                server.setAll(config);
                            }
                        } else {
                            System.out.println(name + File.separator + "Creator > Skipping template that requires extra versioning: " + other);
                        }
                    } else {
                        System.out.println(name + File.separator + "Creator > Skipping disabled template: " + other);
                    }
                } else {
                    System.out.println(name + File.separator + "Creator > Skipping missing template: " + other);
                }
            }
            server.setAll(template.getConfigOptions());
            try {
                System.out.println(name + File.separator + "Creator > Loading Template: " + template.getDisplayName());
                Util.copyDirectory(template.getDirectory(), dir);
                var.put("name", name);
                var.put("host", host.getName());
                var.put("template", template.getName());
                var.put("type", template.getType().toString().toUpperCase());
                if (version != null) var.put("version", version.toString());
                var.put("address", host.getAddress().getHostAddress());
                var.put("port", Integer.toString(port));
                switch (template.getType()) {
                    case SPONGE:
                    case FORGE:
                        if (version != null) {
                            System.out.println(name + File.separator + "Creator > Searching Versions...");
                            ObjectMap<String> spversionmanifest = new ObjectMap<String>(new Gson().fromJson("{\"versions\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://dl-api.spongepowered.org/v1/org.spongepowered/sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "/downloads?type=stable&minecraft=" + version).openStream(), Charset.forName("UTF-8")))) + '}', Map.class));

                            ObjectMap<String> spprofile = null;
                            Version spversion = null;
                            for (ObjectMap<String> profile : spversionmanifest.getMapList("versions")) {
                                if (profile.getMap("dependencies").getRawString("minecraft").equalsIgnoreCase(version.toString()) && (spversion == null || new Version(profile.getRawString("version")).compareTo(spversion) >= 0)) {
                                    spprofile = profile;
                                    spversion = new Version(profile.getRawString("version"));
                                }
                            }
                            if (spversion == null)
                                throw new InvalidServerException("Cannot find Sponge version for Minecraft " + version.toString());
                            System.out.println(name + File.separator + "Creator > Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"');

                            if (template.getType() == ServerType.FORGE) {
                                Version mcfversion = new Version(((spprofile.getMap("dependencies").getRawString("forge").contains("-"))?"":spprofile.getMap("dependencies").getRawString("minecraft") + '-') + spprofile.getMap("dependencies").getRawString("forge"));
                                System.out.println(name + File.separator + "Creator > Found \"forge-" + mcfversion.toString() + '"');

                                var.put("mcf_version", mcfversion.toString());
                            }
                            var.put("sp_version", spversion.toString());
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (template.getBuildOptions().contains("Executable")) {
                File cache;
                if (template.getBuildOptions().getBoolean("Use-Cache", true)) {
                    cache = new UniversalFile(host.plugin.dir, "SubServers:Cache:Templates:" + template.getName());
                    cache.mkdirs();
                    String c = cache.toString();
                    if (System.getProperty("os.name").toLowerCase().startsWith("windows") &&
                            (template.getBuildOptions().getRawString("Executable").toLowerCase().startsWith("bash ") || template.getBuildOptions().getRawString("Executable").toLowerCase().startsWith("sh "))) c = c.replace(File.separatorChar, '/');
                    var.put("cache", c);
                } else {
                    cache = null;
                }

                try {
                    System.out.println(name + File.separator + "Creator > Launching Build Script...");
                    ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(gitBash, template.getBuildOptions().getRawString("Executable"))).directory(dir);
                    pb.environment().putAll(var);
                    process = pb.start();
                    log.file = new File(dir, "SubCreator-" + template.getName() + ((version != null)?"-"+version.toString():"") + ".log");
                    log.process = process;
                    log.start();

                    process.waitFor();
                    Thread.sleep(500);

                    if (process.exitValue() != 0) error = true;
                } catch (InterruptedException e) {
                    error = true;
                } catch (Exception e) {
                    error = true;
                    e.printStackTrace();
                }

                if (cache != null) {
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new UniversalFile(host.plugin.dir, "SubServers:Cache:Templates");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new UniversalFile(host.plugin.dir, "SubServers:Cache");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                }
            }

            new UniversalFile(dir, "template.yml").delete();
            if (error) throw new SubCreatorException();
            return server;
        }

        public void run() {
            UniversalFile dir = new UniversalFile(new File(host.getPath()), name);
            dir.mkdirs();
            ObjectMap<String> server = new ObjectMap<String>();
            ObjectMap<String> config;
            try {
                config = build(dir, template, new LinkedList<>());
                generateProperties(dir, port);
                generateClient(dir, template.getType(), name);
            } catch (SubCreatorException e) {
                config = null;
            } catch (Exception e) {
                config = null;
                e.printStackTrace();
            }

            if (config != null) {
                try {
                    System.out.println(name + File.separator + "Creator > Saving...");
                    if (host.plugin.exServers.keySet().contains(name.toLowerCase()))
                        host.plugin.exServers.remove(name.toLowerCase());

                    config = new ObjectMap<String>((Map<String, ?>) convert(config.get(), new NamedContainer<>("$player$", (player == null)?"":player.toString()), new NamedContainer<>("$name$", name),
                            new NamedContainer<>("$template$", template.getName()), new NamedContainer<>("$type$", template.getType().toString()), new NamedContainer<>("$version$", version.toString().replace(" ", "@")),
                            new NamedContainer<>("$address$", host.getAddress().getHostAddress()), new NamedContainer<>("$port$", Integer.toString(port))));

                    server.set("Enabled", true);
                    server.set("Display", "");
                    server.set("Host", host.getName());
                    server.set("Group", new ArrayList<String>());
                    server.set("Port", port);
                    server.set("Motd", "Some SubServer");
                    server.set("Log", true);
                    server.set("Directory", "." + File.separatorChar + name);
                    server.set("Executable", "java -Xmx1024M -jar " + template.getType().toString() + ".jar");
                    server.set("Stop-Command", "stop");
                    server.set("Stop-Action", "NONE");
                    server.set("Run-On-Launch", false);
                    server.set("Restricted", false);
                    server.set("Incompatible", new ArrayList<String>());
                    server.set("Hidden", false);
                    server.setAll(config);

                    SubServer subserver = host.addSubServer(player, name, server.getBoolean("Enabled"), port, ChatColor.translateAlternateColorCodes('&', server.getString("Motd")), server.getBoolean("Log"), server.getRawString("Directory"),
                            server.getRawString("Executable"), server.getRawString("Stop-Command"), server.getBoolean("Hidden"), server.getBoolean("Restricted"));
                    if (server.getString("Display").length() > 0) subserver.setDisplayName(server.getString("Display"));
                    for (String group : server.getStringList("Group")) subserver.addGroup(group);
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(server.getRawString("Stop-Action").toUpperCase().replace('-', '_').replace(' ', '_')), null);
                    if (action != null) subserver.setStopAction(action);
                    if (server.contains("Extra")) for (String extra : server.getMap("Extra").getKeys())
                        subserver.addExtra(extra, server.getMap("Extra").getObject(extra));
                    host.plugin.servers.get().getMap("Servers").set(name, server);
                    host.plugin.servers.save();
                    if (template.getBuildOptions().getBoolean("Run-On-Finish", true))
                        subserver.start();

                    InternalSubCreator.this.thread.remove(name.toLowerCase());
                    callback.run(subserver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(name + File.separator + "Creator > Couldn't build the server jar. Check the SubCreator logs for more detail.");
            }
            InternalSubCreator.this.thread.remove(name.toLowerCase());
        } private Object convert(Object value, NamedContainer<String, String>... replacements) {
            if (value instanceof Map) {
                List<String> list = new ArrayList<String>();
                list.addAll(((Map<String, Object>) value).keySet());
                for (String key : list) ((Map<String, Object>) value).put(key, convert(((Map<String, Object>) value).get(key), replacements));
                return value;
            } else if (value instanceof Collection) {
                List<Object> list = new ArrayList<Object>();
                for (Object val : (Collection<Object>) value) list.add(convert(val, replacements));
                return list;
            } else if (value.getClass().isArray()) {
                List<Object> list = new ArrayList<Object>();
                for (int i = 0; i < ((Object[]) value).length; i++) list.add(convert(((Object[]) value)[i], replacements));
                return list;
            } else if (value instanceof String) {
                return replace((String) value, replacements);
            } else {
                return value;
            }
        } private String replace(String string, NamedContainer<String, String>... replacements) {
            for (NamedContainer<String, String> replacement : replacements) string = string.replace(replacement.name(), replacement.get());
            return string;
        }
    }

    /**
     * Creates an Internal SubCreator
     *
     * @param host Host
     * @param ports The range of ports to auto-select from
     * @param log Whether SubCreator should log to console
     * @param gitBash The Git Bash directory
     */
    public InternalSubCreator(InternalHost host, Range<Integer> ports, boolean log, String gitBash) {
        if (!ports.hasLowerBound() || !ports.hasUpperBound()) throw new IllegalArgumentException("Port range is not bound");
        if (Util.isNull(host, ports, log, gitBash)) throw new NullPointerException();
        this.host = host;
        this.ports = ports;
        this.log = new Container<Boolean>(log);
        this.gitBash = (System.getenv("ProgramFiles(x86)") == null)?Pattern.compile("%(ProgramFiles)\\(x86\\)%", Pattern.CASE_INSENSITIVE).matcher(gitBash).replaceAll("%$1%"):gitBash;
        if (this.gitBash.endsWith(File.pathSeparator)) this.gitBash = this.gitBash.substring(0, this.gitBash.length() - 1);
        this.thread = new TreeMap<String, CreatorTask>();
        reload();
    }

    @Override
    public void reload() {
        templates.clear();
        if (new UniversalFile(host.plugin.dir, "SubServers:Templates").exists())
            for (File file : new UniversalFile(host.plugin.dir, "SubServers:Templates").listFiles()) {
                try {
                    if (file.isDirectory() && !file.getName().endsWith(".x")) {
                        ObjectMap<String> config = (new UniversalFile(file, "template.yml").exists()) ? new YAMLConfig(new UniversalFile(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()) : new ObjectMap<String>();
                        ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()));
                        templates.put(file.getName().toLowerCase(), template);
                        if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                    }
                } catch (Exception e) {
                    System.out.println(host.getName() + File.separator + "Creator > Couldn't load template: " + file.getName());
                    e.printStackTrace();
                }
            }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Callback<SubServer> callback) {
        if (Util.isNull(name, template)) throw new NullPointerException();
        if (host.isAvailable() && host.isEnabled() && template.isEnabled() && !SubAPI.getInstance().getSubServers().keySet().contains(name.toLowerCase()) && !SubCreator.isReserved(name) && (version != null || !template.requiresVersion())) {
            StackTraceElement[] origin = new Exception().getStackTrace();

            if (port == null) {
                Container<Integer> i = new Container<Integer>(ports.lowerEndpoint() - 1);
                port = Util.getNew(getAllReservedAddresses(), () -> {
                    do {
                        i.set(i.get() + 1);
                        if (i.get() > ports.upperEndpoint()) throw new IllegalStateException("There are no more ports available in range: " + ports.toString());
                    } while (!ports.contains(i.get()));
                    return new InetSocketAddress(host.getAddress(), i.get());
                }).getPort();
            }

            CreatorTask task = new CreatorTask(player, name, template, version, port, server -> {
                if (callback != null && server != null) try {
                    callback.run(server);
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            });
            this.thread.put(name.toLowerCase(), task);

            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                task.start();
                return true;
            } else {
                this.thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    }

    @Override
    public void terminate() {
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            terminate(i);
        }
    }

    @Override
    public void terminate(String name) {
        if (this.thread.keySet().contains(name.toLowerCase())) {
            if (this.thread.get(name.toLowerCase()).process != null && this.thread.get(name.toLowerCase()).process.isAlive()) {
                Executable.terminate(this.thread.get(name.toLowerCase()).process);
            } else if (this.thread.get(name.toLowerCase()).isAlive()) {
                this.thread.get(name.toLowerCase()).interrupt();
                this.thread.remove(name.toLowerCase());
            }
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            waitFor(i);
        }
    }

    @Override
    public void waitFor(String name) throws InterruptedException {
        while (this.thread.keySet().contains(name.toLowerCase()) && this.thread.get(name.toLowerCase()).isAlive()) {
            Thread.sleep(250);
        }
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public Range getPortRange() {
        return ports;
    }

    @Override
    public void setPortRange(Range<Integer> value) {
        if (!value.hasLowerBound() || !value.hasUpperBound()) throw new IllegalArgumentException("Port range is not bound");
        ports = value;
    }

    @Override
    public String getBashDirectory() {
        return gitBash;
    }

    @Override
    public List<SubLogger> getLoggers() {
        List<SubLogger> loggers = new ArrayList<SubLogger>();
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    @Override
    public SubLogger getLogger(String name) {
        return this.thread.get(name.toLowerCase()).log;
    }

    @Override
    public boolean isLogging() {
        return log.get();
    }

    @Override
    public void setLogging(boolean value) {
        if (Util.isNull(value)) throw new NullPointerException();
        log.set(value);
    }

    @Override
    public List<String> getReservedNames() {
        return new ArrayList<String>(thread.keySet());
    }

    @Override
    public List<Integer> getReservedPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        for (CreatorTask task : thread.values()) ports.add(task.port);
        return ports;
    }

    @Override
    public Map<String, ServerTemplate> getTemplates() {
        return new TreeMap<String, ServerTemplate>(templates);
    }

    @Override
    public ServerTemplate getTemplate(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getTemplates().get(name.toLowerCase());
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        if (new UniversalFile(dir, "subservers.client").exists()) {
            Files.delete(new UniversalFile(dir, "subservers.client").toPath());
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.FORGE || type == ServerType.SPONGE) {
                if (!new UniversalFile(dir, "mods").exists()) new UniversalFile(dir, "mods").mkdirs();
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new UniversalFile(dir, "mods:SubServers.Client.jar").getPath());
            }
            YAMLSection config = new YAMLSection();
            FileWriter writer = new FileWriter(new UniversalFile(dir, "subdata.json"), false);
            config.set("Name", name);
            config.set("Address", host.plugin.config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1").replace("0.0.0.0", "127.0.0.1"));
            config.set("Password", host.plugin.config.get().getMap("Settings").getMap("SubData").getRawString("Password", ""));
            writer.write(config.toJSON().toString());
            writer.close();

            if (new UniversalFile("SubServers:subdata.rsa.key").exists()) {
                Files.copy(new UniversalFile("SubServers:subdata.rsa.key").toPath(), new UniversalFile(dir, "subdata.rsa.key").toPath());
            }
        }
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
        if (!file.exists()) file.createNewFile();
        InputStream stream = new FileInputStream(file);
        String content = Util.readAll(new BufferedReader(new InputStreamReader(stream))).replaceAll("server-port=.*(\r?\n)", "server-port=" + port + "$1").replaceAll("server-ip=.*(\r?\n)", "server-ip=" + host.getAddress().getHostAddress() + "$1");
        stream.close();
        file.delete();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(content);
        writer.close();
    }
}