package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubCreatedEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Host.SubServer.StopAction;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Bungee.Library.ReplacementScanner;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import net.md_5.bungee.api.ChatColor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * Internal SubCreator Class
 */
@SuppressWarnings("unchecked")
public class InternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private InternalHost host;
    private Range<Integer> ports;
    private Value<Boolean> log;
    private String gitBash;
    private TreeMap<String, CreatorTask> thread;

    private class CreatorTask extends Thread {
        private final UUID player;
        private final SubServer update;
        private final String name;
        private final ServerTemplate template;
        private final Version version;
        private final int port;
        private final String prefix;
        private final InternalSubLogger log;
        private final LinkedList<String> replace;
        private final HashMap<String, String> replacements;
        private final Consumer<SubServer> callback;
        private Boolean install;
        private Process process;

        private CreatorTask(UUID player, String name, ServerTemplate template, Version version, int port, Consumer<SubServer> callback) {
            super("SubServers.Bungee::Internal_SubCreator_Process_Handler(" + name + ')');
            this.player = player;
            this.update = null;
            this.name = name;
            this.template = template;
            this.version = version;
            this.port = port;
            this.log = new InternalSubLogger(null, this, prefix = name + File.separator + "Creator", InternalSubCreator.this.log, null);
           (this.replace = new LinkedList<String>()).add("/server.properties");
            this.replacements = new HashMap<String, String>();
            this.callback = callback;
        }

        private CreatorTask(UUID player, SubServer server, ServerTemplate template, Version version, Consumer<SubServer> callback) {
            super("SubServers.Bungee::Internal_SubCreator_Process_Handler(" + server.getName() + ')');
            this.player = player;
            this.update = server;
            this.name = server.getName();
            this.template = template;
            this.version = version;
            this.port = server.getAddress().getPort();
            this.log = new InternalSubLogger(null, this, prefix = name + File.separator + "Updater", InternalSubCreator.this.log, null);
           (this.replace = new LinkedList<String>()).add("/server.properties");
            this.replacements = new HashMap<String, String>();
            this.callback = callback;
        }

        private ObjectMap<String> build(File dir, ServerTemplate template, List<ServerTemplate> history, List<ServerTemplate> stack) throws SubCreatorException {
            ObjectMap<String> server = new ObjectMap<String>();
            Version version = this.version;
            HashMap<String, String> var = new HashMap<String, String>();
            boolean error = false;
            if (stack.contains(template)) throw new IllegalStateException("Infinite template import loop detected");
            stack.add(template);
            for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
                if (templates.containsKey(other.toLowerCase())) {
                    final ServerTemplate ot = templates.get(other.toLowerCase());
                    if (ot.isEnabled()) {
                        if (version != null || !ot.requiresVersion()) {
                            if (update == null || ot.canUpdate()) {
                                if (!history.contains(ot)) {
                                    server.setAll(this.build(dir, ot, history, stack));
                                } else {
                                    log.log(WARNING, "Skipping template that is already loaded: " + other);
                                }
                            } else {
                                log.log(WARNING, "Skipping template that cannot be run in update mode: " + other);
                            }
                        } else {
                            log.log(WARNING, "Skipping template that requires extra versioning information: " + other);
                        }
                    } else {
                        log.log(WARNING, "Skipping disabled template: " + other);
                    }
                } else {
                    log.log(WARNING, "Skipping missing template: " + other);
                }
            }
            history.add(template);
            stack.remove(template);
            server.setAll(template.getConfigOptions());
            try {
                log.log(INFO, "Loading" + ((template.isDynamic())?" Dynamic":"") + " Template: " + template.getDisplayName());
                updateDirectory(template.getDirectory(), dir, template.getBuildOptions().getBoolean("Update-Files", false));

                install = template.getBuildOptions().getBoolean("Install-Client", install);
                replace.addAll(template.getBuildOptions().getStringList("Replace", Collections.emptyList()));
                for (ObjectMapValue<String> replacement : template.getBuildOptions().getMap("Replacements", new ObjectMap<>()).getValues()) if (!replacement.isNull()) {
                    replacements.put(replacement.getHandle().toLowerCase().replace('-', '_').replace(' ', '_'), replacement.asString());
                }

                var.putAll(replacements);
                var.put("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
                var.put("mode", (update == null)? "CREATE" : ((CreatorTask.this.template.equals(update.getTemplate()))?"UPDATE":"SWITCH"));
                if (player != null) var.put("player", player.toString().toUpperCase());
                else var.remove("player");
                var.put("name", name);
                var.put("host", host.getName());
                var.put("template", template.getName());
                var.put("type", template.getType().toString().toUpperCase());
                if (version != null) var.put("version", version.toString());
                else var.remove("version");
                var.put("address", host.getAddress().getHostAddress());
                var.put("port", Integer.toString(port));
                switch (template.getType()) {
                    case SPONGE:
                    case FORGE:
                        if (version != null) {
                            log.log(INFO, "Searching Versions...");
                            ObjectMap<String> spversionmanifest = new ObjectMap<String>(new Gson().fromJson("{\"versions\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://dl-api.spongepowered.org/v1/org.spongepowered/sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "/downloads?type=stable&minecraft=" + version).openStream(), Charset.forName("UTF-8")))) + '}', Map.class));

                            ObjectMap<String> spprofile = null;
                            Version spversion = null;
                            for (ObjectMap<String> profile : spversionmanifest.getMapList("versions")) {
                                if (profile.getMap("dependencies").getString("minecraft").equalsIgnoreCase(version.toString()) && (spversion == null || new Version(profile.getString("version")).compareTo(spversion) >= 0)) {
                                    spprofile = profile;
                                    spversion = new Version(profile.getString("version"));
                                }
                            }
                            if (spversion == null)
                                throw new InvalidServerException("Cannot find Sponge version for Minecraft " + version.toString());
                            log.log(INFO, "Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"');

                            if (template.getType() == ServerType.FORGE) {
                                Version mcfversion = new Version(((spprofile.getMap("dependencies").getString("forge").contains("-"))?"":spprofile.getMap("dependencies").getString("minecraft") + '-') + spprofile.getMap("dependencies").getString("forge"));
                                log.log(INFO, "Found \"forge-" + mcfversion.toString() + '"');

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
                File cache = null;
                if (template.getBuildOptions().getBoolean("Use-Cache", true)) {
                    cache = new File(host.plugin.dir, "SubServers/Cache/Templates/" + template.getName());
                    cache.mkdirs();
                    var.put("cache", cache.getAbsolutePath());
                }
                var.put("source", dir.getAbsolutePath());

                try {
                    log.log(INFO, "Launching Build Script...");
                    ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(gitBash, template.getBuildOptions().getString("Executable"))).directory(dir);
                    pb.environment().putAll(var);
                    log.file = new File(dir, "SubCreator-" + template.getName() + ((version != null)?"-"+version.toString():"") + ".log");
                    process = pb.start();
                    log.process = process;
                    log.start();

                    process.waitFor();
                    Thread.sleep(250);

                    if (process.exitValue() != 0) error = true;
                } catch (InterruptedException e) {
                    error = true;
                } catch (Exception e) {
                    error = true;
                    e.printStackTrace();
                }

                if (cache != null) {
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new File(host.plugin.dir, "SubServers/Cache/Templates");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new File(host.plugin.dir, "SubServers/Cache");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                }
            }

            new File(dir, "template.yml").delete();
            if (error) throw new SubCreatorException();
            return server;
        }

        public void run() {
            Runnable declaration = () -> {
                replacements.put("player", (player == null)?"":player.toString());
                replacements.put("name", name);
                replacements.put("host", host.getName());
                replacements.put("template", template.getName());
                replacements.put("type", template.getType().toString());
                replacements.put("version", (version != null)?version.toString():"");
                replacements.put("address", host.getAddress().getHostAddress());
                replacements.put("port", Integer.toString(port));
            };

            declaration.run();
            File dir = (update != null)?new File(update.getFullPath()):new File(host.getPath(),
                    (template.getConfigOptions().contains("Directory"))?new ReplacementScanner(replacements).replace(template.getConfigOptions().getString("Directory")).toString():name);

            ObjectMap<String> server = new ObjectMap<String>();
            ObjectMap<String> config;
            try {
                log.init();
                config = build(dir, template, new LinkedList<>(), new LinkedList<>());
            } catch (SubCreatorException e) {
                config = null;
            } catch (Exception e) {
                config = null;
                e.printStackTrace();
            } finally {
                log.destroy();
            }

            declaration.run();
            ReplacementScanner replacements = new ReplacementScanner(this.replacements);
            if (config != null) {
                try {
                    if (install != Boolean.FALSE) generateClient(dir, template.getType(), name);
                    replacements.replace(dir, replace.toArray(new String[0]));
                } catch (Exception e) {
                    config = null;
                    e.printStackTrace();
                }
            }

            if (config != null) {
                try {
                    Logger.get(prefix).info("Saving...");
                    SubServer subserver = update;
                    if (update == null || update.getTemplate() != template || template.getBuildOptions().getBoolean("Update-Settings", false)) {
                        if (host.plugin.exServers.containsKey(name.toLowerCase()))
                            host.plugin.exServers.remove(name.toLowerCase());

                        config = new ObjectMap<String>((Map<String, ?>) replacements.replace(config.get()));
                        if (config.contains("Directory") && (update != null || !template.getConfigOptions().contains("Directory"))) config.remove("Directory");

                        if (update == null) {
                            server.set("Enabled", true);
                            server.set("Display", "");
                            server.set("Host", host.getName());
                            server.set("Template", template.getName());
                            server.set("Group", new ArrayList<String>());
                            server.set("Port", port);
                            server.set("Motd", "Some SubServer");
                            server.set("Log", true);
                            server.set("Directory", "./" + name);
                            server.set("Executable", "java -Xmx1024M -jar " + template.getType().toString() + ".jar");
                            server.set("Stop-Command", "stop");
                            server.set("Stop-Action", "NONE");
                            server.set("Run-On-Launch", false);
                            server.set("Restricted", false);
                            server.set("Incompatible", new ArrayList<String>());
                            server.set("Hidden", false);
                        } else {
                            server.setAll(host.plugin.servers.get().getMap("Servers").getMap(name, new HashMap<>()));
                            server.set("Template", template.getName());
                        }
                        server.setAll(config);

                        if (update != null) Try.all.run(() -> update.getHost().forceRemoveSubServer(name));
                        subserver = host.constructSubServer(name, server.getBoolean("Enabled"), port, ChatColor.translateAlternateColorCodes('&', Util.unescapeJavaString(server.getString("Motd"))), server.getBoolean("Log"),
                                server.getString("Directory"), server.getString("Executable"), server.getString("Stop-Command"), server.getBoolean("Hidden"), server.getBoolean("Restricted"));

                        if (server.getString("Display").length() > 0) subserver.setDisplayName(Util.unescapeJavaString(server.getString("Display")));
                        subserver.setTemplate(server.getString("Template"));
                        for (String group : server.getStringList("Group")) subserver.addGroup(group);
                        SubServer.StopAction action = Try.all.get(() -> SubServer.StopAction.valueOf(server.getString("Stop-Action").toUpperCase().replace('-', '_').replace(' ', '_')));
                        if (action != null) subserver.setStopAction(action);
                        if (server.contains("Extra")) for (String extra : server.getMap("Extra").getKeys())
                            subserver.addExtra(extra, server.getMap("Extra").getObject(extra));

                        if ((update != null && host.plugin.servers.get().getMap("Servers").contains(name)) ||
                                !(subserver.getStopAction() == StopAction.REMOVE_SERVER || subserver.getStopAction() == StopAction.RECYCLE_SERVER || subserver.getStopAction() == StopAction.DELETE_SERVER)) {
                            host.plugin.servers.get().getMap("Servers").set(name, server);
                            host.plugin.servers.save();
                        }

                        host.addSubServer(subserver);
                        if (update == null && template.getBuildOptions().getBoolean("Run-On-Finish", true))
                            subserver.start();
                    }

                    InternalSubCreator.this.thread.remove(name.toLowerCase());

                    host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, subserver, update != null, true));
                    callback.accept(subserver);
                } catch (Exception e) {
                    e.printStackTrace();
                    host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, update, update != null, false));
                    callback.accept(null);
                }
            } else {
                Logger.get(prefix).info("Couldn't build the server jar. Check the SubCreator logs for more detail.");
                host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, update, update != null, false));
                callback.accept(null);
            }
            InternalSubCreator.this.thread.remove(name.toLowerCase());
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
        Util.nullpo(host, ports, log, gitBash);
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
        if (new File(host.plugin.dir, "SubServers/Templates").exists())
            for (File file : new File(host.plugin.dir, "SubServers/Templates").listFiles()) {
                try {
                    if (file.isDirectory() && !file.getName().endsWith(".x")) {
                        ObjectMap<String> config = (new File(file, "template.yml").exists())? new YAMLConfig(new File(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()) : new ObjectMap<String>();
                        ServerTemplate template = loadTemplate(file.getName(), config.getBoolean("Enabled", true), config.getBoolean("Internal", false), config.getString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()));
                        templates.put(file.getName().toLowerCase(), template);
                        if (config.getKeys().contains("Display")) template.setDisplayName(Util.unescapeJavaString(config.getString("Display")));
                    }
                } catch (Exception e) {
                    Logger.get(host.getName()).severe("Couldn't load template: " + file.getName());
                    e.printStackTrace();
                }
            }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Consumer<SubServer> callback) {
        Util.nullpo(name, template);
        if (host.isAvailable() && host.isEnabled() && template.isEnabled() && !SubAPI.getInstance().getSubServers().containsKey(name.toLowerCase()) && !SubCreator.isReserved(name) && (version != null || !template.requiresVersion())) {
            StackTraceElement[] origin = new Throwable().getStackTrace();

            if (port == null) {
                Value<Integer> i = new Container<Integer>(ports.lowerEndpoint() - 1);
                port = Util.getNew(getAllReservedAddresses(), () -> {
                    do {
                        i.value(i.value() + 1);
                        if (i.value() > ports.upperEndpoint()) throw new IllegalStateException("There are no more ports available in range: " + ports.toString());
                    } while (!ports.contains(i.value()));
                    return new InetSocketAddress(host.getAddress(), i.value());
                }).getPort();
            }

            CreatorTask task = new CreatorTask(player, name, template, version, port, server -> {
                if (callback != null) try {
                    callback.accept(server);
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean update(UUID player, SubServer server, ServerTemplate template, Version version, Consumer<Boolean> callback) {
        Util.nullpo(server);
        final ServerTemplate ft = (template == null)?server.getTemplate():template;
        if (host.isAvailable() && host.isEnabled() && host == server.getHost() && server.isAvailable() && !server.isRunning() && ft != null && ft.isEnabled() && ft.canUpdate() && (version != null || !ft.requiresVersion())) {
            StackTraceElement[] origin = new Throwable().getStackTrace();

            ((InternalSubServer) server).updating(true);
            CreatorTask task = new CreatorTask(player, server, ft, version, x -> {
                ((InternalSubServer) server).updating(false);
                if (callback != null) try {
                    callback.accept(x != null);
                } catch (Throwable e) {
                    Throwable ew = new InvocationTargetException(e);
                    ew.setStackTrace(origin);
                    ew.printStackTrace();
                }
            });
            this.thread.put(server.getName().toLowerCase(), task);

            final SubCreateEvent event = new SubCreateEvent(player, server, ft, version);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                task.start();
                return true;
            } else {
                this.thread.remove(server.getName().toLowerCase());
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
        if (this.thread.containsKey(name.toLowerCase())) {
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
        while (this.thread.containsKey(name.toLowerCase()) && this.thread.get(name.toLowerCase()).isAlive()) {
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
        return log.value();
    }

    @Override
    public void setLogging(boolean value) {
        Util.nullpo(value);
        log.value(value);
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
        TreeMap<String, ServerTemplate> map = new TreeMap<String, ServerTemplate>();
        for (Map.Entry<String, ServerTemplate> template : templates.entrySet()) {
            if (!template.getValue().isInternal()) map.put(template.getKey(), template.getValue());
        }
        return map;
    }

    @Override
    public ServerTemplate getTemplate(String name) {
        Util.nullpo(name);

        ServerTemplate template = templates.getOrDefault(name.toLowerCase(), null);
        if (template == null || template.isInternal()) {
            return null;
        } else {
            return template;
        }
    }

    private static Pair<YAMLSection, Map<String, Object>> subdata = null;
    private Map<String, Object> getSubData() {
        if (subdata == null || host.plugin.config.get() != subdata.key()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("Address", host.plugin.config.get().getMap("Settings").getMap("SubData").getString("Address", "127.0.0.1").replace("0.0.0.0", "127.0.0.1"));
            if (host.plugin.config.get().getMap("Settings").getMap("SubData").getString("Password", "").length() > 0) map.put("Password", host.plugin.config.get().getMap("Settings").getMap("SubData").getString("Password"));
            subdata = new ContainedPair<>(host.plugin.config.get(), map);
        }
        return subdata.value();
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        boolean installed = false;
        if (type == ServerType.SPIGOT) {
            installed = true;
            if (!new File(dir, "plugins").exists()) new File(dir, "plugins").mkdirs();
            if (!new File(dir, "plugins/SubServers.Client.jar").exists())
                Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new File(dir, "plugins/SubServers.Client.jar").getPath());
        } else if (type == ServerType.FORGE || type == ServerType.SPONGE) {
            installed = true;
            if (!new File(dir, "mods").exists()) new File(dir, "mods").mkdirs();
            if (!new File(dir, "mods/SubServers.Client.jar").exists())
                Util.copyFromJar(SubProxy.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/client.jar", new File(dir, "mods/SubServers.Client.jar").getPath());
        }

        if (installed) {
            YAMLSection config = new YAMLSection();
            FileWriter writer = new FileWriter(new File(dir, "subdata.json"), false);
            config.setAll(getSubData());
            writer.write(new Gson().toJson(config.get()));
            writer.close();

            if (!new File(dir, "subdata.rsa.key").exists() && new File("SubServers/subdata.rsa.key").exists()) {
                Files.copy(new File("SubServers/subdata.rsa.key").toPath(), new File(dir, "subdata.rsa.key").toPath());
            }
        }
    }

    private void updateDirectory(File from, File to, boolean overwrite) {
        if (!to.exists()) {
            Directories.copy(from, to);
        } else if (from.isDirectory() && !Files.isSymbolicLink(from.toPath())) {
            String files[] = from.list();

            for (String file : files) {
                File srcFile = new File(from, file);
                File destFile = new File(to, file);

                updateDirectory(srcFile, destFile, overwrite);
            }
        } else {
            try {
                if (overwrite && (from.length() != to.length() || !Arrays.equals(generateSHA256(to), generateSHA256(from)))) {
                    if (to.exists()) {
                        if (to.isDirectory()) Directories.delete(to);
                        else to.delete();
                    }
                    Files.copy(from.toPath(), to.toPath(), LinkOption.NOFOLLOW_LINKS, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } private byte[] generateSHA256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(file);
        byte[] dataBytes = new byte[4096];

        int nread;

        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }

        fis.close();
        return md.digest();
    }
}