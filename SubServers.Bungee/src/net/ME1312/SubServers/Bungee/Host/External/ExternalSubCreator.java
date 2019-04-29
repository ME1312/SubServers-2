package net.ME1312.SubServers.Bungee.Host.External;

import com.google.common.collect.Range;
import net.ME1312.Galaxi.Library.*;
import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExConfigureHost;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Bungee.SubAPI;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * External SubCreator Class
 */
@SuppressWarnings("unchecked")
public class ExternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private ExternalHost host;
    private Range<Integer> ports;
    private Container<Boolean> log;
    private String gitBash;
    private TreeMap<String, NamedContainer<Integer, ExternalSubLogger>> thread;

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
        if (Util.isNull(host, ports, log, gitBash)) throw new NullPointerException();
        this.host = host;
        this.ports = ports;
        this.log = new Container<Boolean>(log);
        this.gitBash = gitBash;
        this.thread = new TreeMap<String, NamedContainer<Integer, ExternalSubLogger>>();
        reload();
    }

    @Override
    public void reload() {
        templates.clear();
        if (new UniversalFile(host.plugin.dir, "SubServers:Templates").exists()) for (File file : new UniversalFile(host.plugin.dir, "SubServers:Templates").listFiles()) {
            try {
                if (file.isDirectory() && !file.getName().endsWith(".x")) {
                    ObjectMap<String> config = (new UniversalFile(file, "template.yml").exists())?new YAMLConfig(new UniversalFile(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()):new ObjectMap<String>();
                    ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()));
                    templates.put(file.getName().toLowerCase(), template);
                    if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                }
            } catch (Exception e) {
                System.out.println(host.getName() + "/Creator > Couldn't load template: " + file.getName());
                e.printStackTrace();
            }
        }
        if (host.client.name()) host.queue(new PacketExConfigureHost(host.plugin, host));
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, Integer port, Callback<SubServer> callback) {
        if (Util.isNull(name, template)) throw new NullPointerException();
        if (host.isAvailable() && host.isEnabled() && template.isEnabled() && !SubAPI.getInstance().getSubServers().keySet().contains(name.toLowerCase()) && !SubCreator.isReserved(name)) {
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
            ExternalSubLogger logger = new ExternalSubLogger(this, name + File.separator + "Creator", log, null);
            thread.put(name.toLowerCase(), new NamedContainer<>(port, logger));

            final int fport = port;
            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                logger.start();
                host.queue(new PacketExCreateServer(name, template, version, port, logger.getExternalAddress(), data -> {
                    try {
                        if (data.getInt(0x0001) == 0) {
                            System.out.println(name + "/Creator > Saving...");
                            if (host.plugin.exServers.keySet().contains(name.toLowerCase()))
                                host.plugin.exServers.remove(name.toLowerCase());

                            ObjectMap<String> server = new ObjectMap<String>();
                            ObjectMap<String> config = new ObjectMap<String>((Map<String, ?>) convert(data.getMap(0x0002).get(), new NamedContainer<>("$player$", (player == null)?"":player.toString()), new NamedContainer<>("$name$", name),
                                    new NamedContainer<>("$template$", template.getName()), new NamedContainer<>("$type$", template.getType().toString()), new NamedContainer<>("$version$", (version != null)?version.toString().replace(" ", "@"):""),
                                    new NamedContainer<>("$address$", new ObjectMap<String>((Map<String, ?>) data.getObject(0x0002)).getRawString("\033address", "null")), new NamedContainer<>("$port$", Integer.toString(fport))));

                            config.remove("\033address");

                            server.set("Enabled", true);
                            server.set("Display", "");
                            server.set("Host", host.getName());
                            server.set("Group", new ArrayList<String>());
                            server.set("Port", fport);
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

                            SubServer subserver = host.addSubServer(player, name, server.getBoolean("Enabled"), fport, ChatColor.translateAlternateColorCodes('&', server.getString("Motd")), server.getBoolean("Log"), server.getRawString("Directory"),
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

                            if (callback != null) try {
                                callback.run(subserver);
                            } catch (Throwable e) {
                                Throwable ew = new InvocationTargetException(e);
                                ew.setStackTrace(origin);
                                ew.printStackTrace();
                            }
                        } else {
                            System.out.println(name + "/Creator > " + data.getString(0x0003));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.stop();
                    this.thread.remove(name.toLowerCase());
                }));
                return true;
            } else {
                thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
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

    @Override
    public void terminate() {
        HashMap<String, NamedContainer<Integer, ExternalSubLogger>> thread = new HashMap<String, NamedContainer<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            terminate(i);
        }
    }

    @Override
    public void terminate(String name) {
        if (this.thread.keySet().contains(name.toLowerCase())) {
            ((SubDataClient) host.getSubData()).sendPacket(new PacketExCreateServer(name.toLowerCase()));
            thread.remove(name.toLowerCase());
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        HashMap<String, NamedContainer<Integer, ExternalSubLogger>> thread = new HashMap<String, NamedContainer<Integer, ExternalSubLogger>>();
        thread.putAll(this.thread);
        for (String i : thread.keySet()) {
            waitFor(i);
        }
    }

    @Override
    public void waitFor(String name) throws InterruptedException {
        while (this.thread.keySet().contains(name.toLowerCase()) && host.client.get() != null) {
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
        HashMap<String, NamedContainer<Integer, ExternalSubLogger>> temp = new HashMap<String, NamedContainer<Integer, ExternalSubLogger>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    @Override
    public SubLogger getLogger(String name) {
        return this.thread.get(name.toLowerCase()).get();
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
        for (NamedContainer<Integer, ExternalSubLogger> task : thread.values()) ports.add(task.name());
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
}
