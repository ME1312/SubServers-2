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
import java.util.UUID;

/**
 * External SubCreator Class
 */
public class ExternalSubCreator extends SubCreator {
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
    public boolean create(UUID player, String name, ServerType type, Version version, int memory, int port) {
        if (Util.isNull(name, type, version, memory, port)) throw new NullPointerException();
        if (!isBusy()) {
            final SubCreateEvent event = new SubCreateEvent(player, host, name, type, version, memory, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                running = true;
                logger.start();
                host.queue(new PacketExCreateServer(name, type, version, memory, port, logger.getExternalAddress(), (JSONCallback) json -> {
                    try {
                        if (json.getInt("r") == 0) {
                            System.out.println(host.getName() + "/Creator > Saving...");
                            if (host.plugin.exServers.keySet().contains(name.toLowerCase())) host.plugin.exServers.remove(name.toLowerCase());
                            SubServer subserver = host.addSubServer(player, name, true, port, "&aThis is a SubServer", true, json.getJSONObject("c").getString("dir"), new Executable(json.getJSONObject("c").getString("exec")), "stop", false, false, false, false, false);

                            YAMLSection server = new YAMLSection();
                            server.set("Enabled", true);
                            server.set("Host", host.getName());
                            server.set("Port", port);
                            server.set("Motd", "&aThis is a SubServer");
                            server.set("Log", true);
                            server.set("Directory", json.getJSONObject("c").getString("dir"));
                            server.set("Executable", json.getJSONObject("c").getString("exec"));
                            server.set("Stop-Command", "stop");
                            server.set("Run-On-Launch", false);
                            server.set("Auto-Restart", false);
                            server.set("Hidden", false);
                            server.set("Restricted", false);
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
}
