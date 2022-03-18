package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Event.SubCreatedEvent;
import net.ME1312.SubServers.Bungee.Host.Host;
import net.ME1312.SubServers.Bungee.Host.SubCreator;
import net.ME1312.SubServers.Bungee.Host.SubLogger;
import net.ME1312.SubServers.Bungee.Host.SubServer;
import net.ME1312.SubServers.Bungee.Host.SubServer.StopAction;
import net.ME1312.SubServers.Bungee.Library.Compatibility.Logger;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExConfigureHost;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExDownloadTemplates;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExUploadTemplates;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.ME1312.SubServers.Bungee.SubProxy;

import com.google.common.collect.Range;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;

/**
 * External SubCreator Class
 */
@SuppressWarnings("unchecked")
public class ExternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private HashMap<String, ServerTemplate> templatesR = new HashMap<String, ServerTemplate>();
    private Boolean enableRT = false;
    private ExternalHost host;
    private Range<Integer> ports;
    private Value<Boolean> log;
    private String gitBash;
    private TreeMap<String, Pair<Integer, ExternalSubLogger>> thread;

    /**
     * Creates an External SubCreator
     *
     * @param host Host
     * @param ports The range of ports to auto-select from
     * @param log Whether SubCreator should log to console
     * @param gitBash The Git Bash directory
     */
    public ExternalSubCreator(ExternalHost host, Range<Integer> ports, boolean log, String gitBash) {
        if (!ports.hasLowerBound() || !ports.hasUpperBound()) throw new IllegalArgumentException("Port range is not bound");
        Util.nullpo(host, ports, log, gitBash);
        this.host = host;
        this.ports = ports;
        this.log = new Container<Boolean>(log);
        this.gitBash = gitBash;
        this.thread = new TreeMap<String, Pair<Integer, ExternalSubLogger>>();
        reload();
    }

    @Override
    public void reload() {
        templatesR.clear();
        if (new File(host.plugin.dir, "SubServers/Templates").exists()) for (File file : new File(host.plugin.dir, "SubServers/Templates").listFiles()) {
            try {
                if (file.isDirectory() && !file.getName().endsWith(".x")) {
                    ObjectMap<String> config = (new File(file, "template.yml").exists())? new YAMLConfig(new File(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()) : new ObjectMap<String>();
                    ServerTemplate template = loadTemplate(file.getName(), config.getBoolean("Enabled", true), config.getBoolean("Internal", false), config.getString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()));
                    templatesR.put(file.getName().toLowerCase(), template);
                    if (config.getKeys().contains("Display")) template.setDisplayName(Util.unescapeJavaString(config.getString("Display")));
                }
            } catch (Exception e) {
                Logger.get(host.getName()).severe("Couldn't load template: " + file.getName());
                e.printStackTrace();
            }
        }

        if (host.available && !Try.all.get(() -> Util.reflect(SubProxy.class.getDeclaredField("reloading"), host.plugin), false)) {
            host.queue(new PacketExConfigureHost(host.plugin, host), new PacketExUploadTemplates(host.plugin, () -> {
                if (enableRT == null || enableRT) host.queue(new PacketExDownloadTemplates(host.plugin, host));
            }));
        }
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Consumer<SubServer> callback) {
        Util.nullpo(name, template);
        if (host.isAvailable() && host.isEnabled() && template.isEnabled() && !SubAPI.getInstance().getSubServers().containsKey(name.toLowerCase()) && !SubCreator.isReserved(name) && (version != null || !template.requiresVersion())) {
            StackTraceElement[] origin = new Throwable().getStackTrace();

            if (port == null) {
                Container<Integer> i = new Container<Integer>(ports.lowerEndpoint() - 1);
                port = Util.getNew(getAllReservedAddresses(), () -> {
                    do {
                        ++i.value;
                        if (i.value > ports.upperEndpoint()) throw new IllegalStateException("There are no more ports available in range: " + ports.toString());
                    } while (!ports.contains(i.value));
                    return new InetSocketAddress(host.getAddress(), i.value);
                }).getPort();
            }
            String prefix = name + File.separator + "Creator";
            ExternalSubLogger logger = new ExternalSubLogger(this, prefix, log, null);
            thread.put(name.toLowerCase(), new ContainedPair<>(port, logger));

            final int fport = port;
            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                logger.start();
                host.queue(new PacketExCreateServer(player, name, template, version, port, logger.getExternalAddress(), data -> {
                    finish(player, null, name, template, version, fport, prefix, origin, data, callback);
                    this.thread.remove(name.toLowerCase());
                }));
                return true;
            } else {
                thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    } private <T> void callback(StackTraceElement[] origin, Consumer<T> callback, T value) {
        if (callback != null) try {
            callback.accept(value);
        } catch (Throwable e) {
            Throwable ew = new InvocationTargetException(e);
            ew.setStackTrace(origin);
            ew.printStackTrace();
        }
    }

    @Override
    public boolean update(UUID player, SubServer server, ServerTemplate template, Version version, Consumer<Boolean> callback) {
        Util.nullpo(server);
        final ServerTemplate ft = (template == null)?server.getTemplate():template;
        if (host.isAvailable() && host.isEnabled() && host == server.getHost() && server.isAvailable() && !server.isRunning() && ft != null && ft.isEnabled() && ft.canUpdate() && (version != null || !ft.requiresVersion())) {
            StackTraceElement[] origin = new Throwable().getStackTrace();

            String name = server.getName();
            String prefix = name + File.separator + "Updater";
            ((ExternalSubServer) server).updating(true);
            ExternalSubLogger logger = new ExternalSubLogger(this, prefix, log, null);
            thread.put(name.toLowerCase(), new ContainedPair<>(server.getAddress().getPort(), logger));

            final SubCreateEvent event = new SubCreateEvent(player, server, ft, version);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                logger.start();
                host.queue(new PacketExCreateServer(player, server, ft, version, logger.getExternalAddress(), data -> {
                    finish(player, server, server.getName(), ft, version, server.getAddress().getPort(), prefix, origin, data, s -> {
                        ((ExternalSubServer) server).updating(false);
                        if (callback != null) callback.accept(s != null);
                    });
                    this.thread.remove(name.toLowerCase());
                }));
                return true;
            } else {
                thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    }

    private void finish(UUID player, SubServer update, String name, ServerTemplate template, Version version, int port, String prefix, StackTraceElement[] origin, ObjectMap<Integer> data, Consumer<SubServer> callback) {
        try {
            if (data.getInt(0x0001) == 0) {
                Logger.get(prefix).info("Saving...");
                SubServer subserver = update;
                if (update == null || update.getTemplate() != template || template.getBuildOptions().getBoolean("Update-Settings", false)) {
                    if (host.plugin.exServers.containsKey(name.toLowerCase()))
                        host.plugin.exServers.remove(name.toLowerCase());

                    ObjectMap<String> server = new ObjectMap<String>();
                    ObjectMap<String> config = new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002));
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
                    if (update == null && template.getBuildOptions().getBoolean("Run-On-Finish", true)) {
                        while (!subserver.isAvailable() && host.isAvailable()) {
                            Thread.sleep(250);
                        }
                        if (subserver.isAvailable()) {
                            subserver.start();
                        }
                    }
                }

                host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, subserver, update != null, true));
                callback(origin, callback, subserver);
            } else {
                Logger.get(prefix).info(data.getString(0x0003));
                host.plugin.getPluginManager().callEvent(new SubCreatedEvent(player, host, name, template, version, port, update, update != null, false));
                callback(origin, callback, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback(origin, callback, null);
        }
    }

    @Override
    public void terminate() {
        HashMap<String, Pair<Integer, ExternalSubLogger>> thread = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            terminate(i);
        }
    }

    @Override
    public void terminate(String name) {
        if (this.thread.containsKey(name.toLowerCase())) {
            ((SubDataClient) host.getSubData()[0]).sendPacket(new PacketExCreateServer(name.toLowerCase()));
            thread.remove(name.toLowerCase());
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        HashMap<String, Pair<Integer, ExternalSubLogger>> thread = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            waitFor(i);
        }
    }

    @Override
    public void waitFor(String name) throws InterruptedException {
        while (this.thread.containsKey(name.toLowerCase()) && host.getSubData()[0] != null) {
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
        HashMap<String, Pair<Integer, ExternalSubLogger>> temp = new HashMap<String, Pair<Integer, ExternalSubLogger>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    @Override
    public SubLogger getLogger(String name) {
        return this.thread.get(name.toLowerCase()).value();
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
        for (Pair<Integer, ExternalSubLogger> task : thread.values()) ports.add(task.key());
        return ports;
    }

    @Override
    public Map<String, ServerTemplate> getTemplates() {
        TreeMap<String, ServerTemplate> map = new TreeMap<String, ServerTemplate>();
        if (enableRT != null && enableRT) for (Map.Entry<String, ServerTemplate> template : templatesR.entrySet()) {
            if (!template.getValue().isInternal()) map.put(template.getKey(), template.getValue());
        }
        for (Map.Entry<String, ServerTemplate> template : templates.entrySet()) {
            if (!template.getValue().isInternal()) map.put(template.getKey(), template.getValue());
        }
        return map;
    }

    @Override
    public ServerTemplate getTemplate(String name) {
        Util.nullpo(name);
        name = name.toLowerCase();

        ServerTemplate template = templates.getOrDefault(name, null);
        if (template == null && enableRT != null && enableRT) template = templatesR.getOrDefault(name, null);
        if (template == null || template.isInternal()) {
            return null;
        } else {
            return template;
        }
    }
}
