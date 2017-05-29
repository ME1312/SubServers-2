package net.ME1312.SubServers.Bungee.Host.External;

import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.JSONCallback;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketExCreateServer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * External SubCreator Class
 */
public class ExternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private ExternalHost host;
    private String gitBash;
    private ExternalSubLogger logger;
    private boolean running;

    /**
     * Creates an External SubCreator
     *
     * @param host Host
     * @param gitBash Git Bash
     */
    public ExternalSubCreator(ExternalHost host, String gitBash) {
        if (Util.isNull(host, gitBash)) throw new NullPointerException();
        this.host = host;
        this.gitBash = gitBash;
        this.logger = new ExternalSubLogger(this, host.getName() + "/Creator", new Container<Boolean>(host.plugin.config.get().getSection("Settings").getBoolean("Log-Creator")), null);
        this.running = false;
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, int port) {
        if (Util.isNull(name, template, version, port)) throw new NullPointerException();
        if (!isBusy()) {
            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                running = true;
                logger.start();
                host.queue(new PacketExCreateServer(name, template, version, port, logger.getExternalAddress(), (JSONCallback) json -> {
                    try {
                        if (json.getInt("r") == 0) {
                            System.out.println(host.getName() + "/Creator > Saving...");
                            if (host.plugin.exServers.keySet().contains(name.toLowerCase())) host.plugin.exServers.remove(name.toLowerCase());

                            YAMLSection server = new YAMLSection(json.getJSONObject("c"));
                            for (String option : server.getKeys()) {
                                if (server.isString(option)) {
                                    server.set(option, server.getRawString(option).replace("$name$", name).replace("$template$", template.getName()).replace("$type$", template.getType().toString())
                                            .replace("$version$", version.toString().replace(" ", "@")).replace("$port$", Integer.toString(port)));
                                }
                            }

                            if (!server.contains("Enabled")) server.set("Enabled", true);
                            if (!server.contains("Host")) server.set("Host", host.getName());
                            if (!server.contains("Port")) server.set("Port", port);
                            if (!server.contains("Motd")) server.set("Motd", "Some SubServer");
                            if (!server.contains("Log")) server.set("Log", true);
                            if (!server.contains("Directory")) server.set("Directory", "." + File.separatorChar + name);
                            if (!server.contains("Executable")) server.set("Executable", "java -Xmx1024M -jar Custom.jar");
                            if (!server.contains("Stop-Command")) server.set("Stop-Command", "stop");
                            if (!server.contains("Run-On-Launch")) server.set("Run-On-Launch", false);
                            if (!server.contains("Auto-Restart")) server.set("Auto-Restart", false);
                            if (!server.contains("Hidden")) server.set("Hidden", false);
                            if (!server.contains("Restricted")) server.set("Restricted", false);

                            SubServer subserver = host.addSubServer(player, name, server.getBoolean("Enabled"), port, server.getColoredString("Motd", '&'), server.getBoolean("Log"), server.getRawString("Directory"),
                                    new Executable(server.getRawString("Executable")), server.getRawString("Stop-Command"), false, server.getBoolean("Auto-Restart"), server.getBoolean("Hidden"), server.getBoolean("Restricted"), false);
                            host.plugin.config.get().getSection("Servers").set(name, server);
                            host.plugin.config.save();

                            subserver.start(player);
                        } else {
                            System.out.println(host.getName() + "/Creator > " + json.getString("m"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.stop();
                    running = false;
                }));
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public void terminate() {
        if (running) {
            host.getSubDataClient().sendPacket(new PacketExCreateServer());
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (running) {
            Thread.sleep(250);
        }
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public String getBashDirectory() {
        return gitBash;
    }

    @Override
    public SubLogger getLogger() {
        return logger;
    }

    @Override
    public boolean isBusy() {
        return running;
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
