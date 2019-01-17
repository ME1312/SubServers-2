package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.Exception.InvalidTemplateException;
import net.ME1312.SubServers.Host.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Host.Network.API.SubCreator.ServerType;
import net.ME1312.SubServers.Host.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Internal SubCreator Class
 */
public class SubCreator {
    private ExHost host;
    private TreeMap<String, CreatorTask> thread;

    public static class ServerTemplate extends net.ME1312.SubServers.Host.Network.API.SubCreator.ServerTemplate {
        private String name;
        private String nick = null;
        private boolean enabled;
        private String icon;
        private File directory;
        private ServerType type;
        private YAMLSection build;
        private YAMLSection options;

        /**
         * Create a SubCreator Template
         *
         * @param name Template Name
         * @param directory Template Directory
         * @param build Build Options
         * @param options Configuration Options
         */
        public ServerTemplate(String name, boolean enabled, String icon, File directory, YAMLSection build, YAMLSection options) {
            super(toRaw(name, enabled, icon, directory, build, options));
            if (name.contains(" ")) throw new InvalidTemplateException("Template names cannot have spaces: " + name);
            this.name = name;
            this.enabled = enabled;
            this.icon = icon;
            this.directory = directory;
            this.type = (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM;
            this.build = build;
            this.options = options;
        }

        /**
         * Get the Name of this Template
         *
         * @return Template Name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the Display Name of this Template
         *
         * @return Display Name
         */
        public String getDisplayName() {
            return (nick == null)?getName():nick;
        }

        /**
         * Sets the Display Name for this Template
         *
         * @param value Value (or null to reset)
         */
        public void setDisplayName(String value) {
            if (value == null || value.length() == 0 || getName().equals(value)) {
                this.nick = null;
            } else {
                this.nick = value;
            }
        }

        /**
         * Get the Enabled Status of this Template
         *
         * @return Enabled Status
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set the Enabled Status of this Template
         *
         * @param value Value
         */
        public void setEnabled(boolean value) {
            enabled = value;
        }

        /**
         * Get the Item Icon for this Template
         *
         * @return Item Icon Name/ID
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Set the Item Icon for this Template
         *
         * @param value Value
         */
        public void setIcon(String value) {
            icon = value;
        }

        /**
         * Get the Directory for this Template
         *
         * @return Directory
         */
        public File getDirectory() {
            return directory;
        }


        /**
         * Get the Type of this Template
         *
         * @return Template Type
         */
        public ServerType getType() {
            return type;
        }

        /**
         * Get the Build Options for this Template
         *
         * @return Build Options
         */
        public YAMLSection getBuildOptions() {
            return build;
        }

        /**
         * Get the Configuration Options for this Template
         *
         * @return Configuration Options
         */
        public YAMLSection getConfigOptions() {
            return options;
        }

        private static YAMLSection toRaw(String name, boolean enabled, String icon, File directory, YAMLSection build, YAMLSection options) {
            if (Util.isNull(name, enabled, directory, build, options)) throw new NullPointerException();
            YAMLSection tinfo = new YAMLSection();
            tinfo.set("enabled", enabled);
            tinfo.set("name", name);
            tinfo.set("display", name);
            tinfo.set("icon", icon);
            tinfo.set("type", (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM);
            return tinfo;
        }
    }

    private class CreatorTask extends Thread {
        private final String name;
        private final ServerTemplate template;
        private final Version version;
        private final int port;
        private final UUID address;
        private final String id;
        private final SubLogger log;
        private Process process;

        private CreatorTask(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
            super(SubAPI.getInstance().getAppInfo().getName() + "::SubCreator_Process_Handler(" + name + ')');
            this.name = name;
            this.template = template;
            this.version = version;
            this.port = port;
            this.log = new SubLogger(null, this, name + File.separator + "Creator", address, new Container<Boolean>(true), null);
            this.address = address;
            this.id = id;
        }

        private YAMLSection build(File dir, ServerTemplate template, List<ServerTemplate> history) throws SubCreatorException {
            YAMLSection server = new YAMLSection();
            Version version = this.version;
            HashMap<String, String> var = new HashMap<String, String>();
            boolean error = false;
            if (history.contains(template)) throw new IllegalStateException("Template Import loop detected");
            history.add(template);
            for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
                if (host.templates.keySet().contains(other.toLowerCase())) {
                    if (host.templates.get(other.toLowerCase()).isEnabled()) {
                        YAMLSection config = build(dir, host.templates.get(other.toLowerCase()), history);
                        if (config == null) {
                            throw new SubCreatorException();
                        } else {
                            server.setAll(config);
                        }
                    } else {
                        log.logger.warn.println("Skipping disabled template: " + other);
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Skipping disabled template: " + other));
                    }
                } else {
                    log.logger.warn.println("Skipping missing template: " + other);
                    host.subdata.sendPacket(new PacketOutExLogMessage(address, "Skipping missing template: " + other));
                }
            }
            server.setAll(template.getConfigOptions());
            try {
                log.logger.info.println("Loading Template: " + template.getDisplayName());
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Loading Template: " + template.getDisplayName()));
                Util.copyDirectory(template.getDirectory(), dir);
                var.put("name", name);
                var.put("template", template.getName());
                var.put("type", template.getType().toString().toUpperCase());
                var.put("version", version.toString());
                var.put("port", Integer.toString(port));
                switch (template.getType()) {
                    case SPONGE:
                    case FORGE:
                        log.logger.info.println("Searching Versions...");
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Searching Versions..."));
                        YAMLSection spversionmanifest = new YAMLSection(new JSONObject("{\"versions\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://dl-api.spongepowered.org/v1/org.spongepowered/sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "/downloads?type=stable&minecraft=" + version).openStream(), Charset.forName("UTF-8")))) + '}'));

                        YAMLSection spprofile = null;
                        Version spversion = null;
                        for (YAMLSection profile : spversionmanifest.getSectionList("versions")) {
                            if (profile.getSection("dependencies").getRawString("minecraft").equalsIgnoreCase(version.toString()) && (spversion == null || new Version(profile.getRawString("version")).compareTo(spversion) >= 0)) {
                                spprofile = profile;
                                spversion = new Version(profile.getRawString("version"));
                            }
                        }
                        if (spversion == null)
                            throw new InvalidServerException("Cannot find Sponge version for Minecraft " + version.toString());
                        log.logger.info.println("Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"');
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"'));

                        if (template.getType() == ServerType.FORGE) {
                            Version mcfversion = new Version(spprofile.getSection("dependencies").getRawString("minecraft") + '-' + spprofile.getSection("dependencies").getRawString("forge"));
                            log.logger.info.println("Found \"forge-" + mcfversion.toString() + '"');
                            host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"forge-" + mcfversion.toString() + '"'));

                            var.put("mcf_version", mcfversion.toString());
                        }
                        var.put("sp_version", spversion.toString());
                        break;
                }
            } catch (Exception e) {
                log.logger.error.println(e);
            }

            if (template.getBuildOptions().contains("Shell-Location")) {
                File cache;
                if (template.getBuildOptions().getBoolean("Use-Cache", true)) {
                    cache = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache:Templates:" + template.getName());
                    cache.mkdirs();
                    String c = cache.toString();
                    if (System.getProperty("os.name").toLowerCase().startsWith("windows") &&
                            (template.getBuildOptions().getRawString("Executable").toLowerCase().startsWith("bash ") || template.getBuildOptions().getRawString("Executable").toLowerCase().startsWith("sh "))) c = c.replace(File.separatorChar, '/');
                    var.put("cache", c);
                } else {
                    cache = null;
                }

                try {
                    log.logger.info.println("Launching Build Script...");
                    host.subdata.sendPacket(new PacketOutExLogMessage(address, "Launching Build Script..."));
                    ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.host.getRawString("Git-Bash"), template.getBuildOptions().getRawString("Executable"))).directory(dir);
                    pb.environment().putAll(var);
                    process = pb.start();
                    log.file = new File(dir, "SubCreator-" + template.getName() + "-" + version.toString().replace(" ", "@") + ".log");
                    log.process = process;
                    log.start();

                    process.waitFor();
                    Thread.sleep(500);

                    if (process.exitValue() != 0) error = true;
                } catch (InterruptedException e) {
                    error = true;
                } catch (Exception e) {
                    error = true;
                    log.logger.error.println(e);
                }

                if (cache != null) {
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache:Templates");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new UniversalFile(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                }
            }

            new UniversalFile(dir, "template.yml").delete();
            if (error) throw new SubCreatorException();
            return server;
        }

        public void run() {
            UniversalFile dir = new UniversalFile(new File(host.host.getRawString("Directory")), name);
            dir.mkdirs();
            YAMLSection server;
            try {
                server = build(dir, template, new LinkedList<>());
                generateProperties(dir, port);
                generateClient(dir, template.getType(), name);
            } catch (SubCreatorException e) {
                server = null;
            } catch (Exception e) {
                server = null;
                log.logger.error.println(e);
            }

            if (server != null) {
                host.subdata.sendPacket(new PacketExCreateServer(0, "Created Server Successfully", template.getConfigOptions(), id));
            } else {
                log.logger.info.println("Couldn't build the server jar. Check the SubCreator logs for more detail.");
                host.subdata.sendPacket(new PacketExCreateServer(-1, "Couldn't build the server jar. Check the SubCreator logs for more detail.", template.getConfigOptions(), id));
            }
            SubCreator.this.thread.remove(name.toLowerCase());
        }
    }

    /**
     * Creates a SubCreator Instance
     *
     * @param host SubServers.Host
     */
    public SubCreator(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
        this.thread = new TreeMap<>();
    }

    public boolean create(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
        if (Util.isNull(name, template, version, port, address)) throw new NullPointerException();
        CreatorTask task = new CreatorTask(name, template, version, port, address, id);
        this.thread.put(name.toLowerCase(), task);
        task.start();
        return true;
    }

    public void terminate() {
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            terminate(i);
        }
    }

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

    public void waitFor() throws InterruptedException {
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            waitFor(i);
        }
    }

    public void waitFor(String name) throws InterruptedException {
        while (this.thread.keySet().contains(name.toLowerCase()) && this.thread.get(name.toLowerCase()).isAlive()) {
            Thread.sleep(250);
        }
    }

    public List<SubLogger> getLoggers() {
        List<SubLogger> loggers = new ArrayList<SubLogger>();
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    public SubLogger getLogger(String name) {
        return this.thread.get(name).log;
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        if (new UniversalFile(dir, "subservers.client").exists()) {
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.FORGE || type == ServerType.SPONGE) {
                if (!new UniversalFile(dir, "mods").exists()) new UniversalFile(dir, "mods").mkdirs();
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(dir, "mods:SubServers.Client.jar").getPath());
            }
            JSONObject config = new JSONObject();
            FileWriter writer = new FileWriter(new UniversalFile(dir, "subservers.client"), false);
            config.put("Name", name);
            config.put("Address", host.config.get().getSection("Settings").getSection("SubData").getRawString("Address"));
            config.put("Password", host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"));
            config.put("Encryption", host.config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE"));
            config.write(writer);
            writer.close();
        }
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
        if (!file.exists()) file.createNewFile();
        FileInputStream is = new FileInputStream(file);
        String content = Util.readAll(new BufferedReader(new InputStreamReader(is))).replaceAll("server-port=.*(\r?\n)", "server-port=" + port + "$1").replaceAll("server-ip=.*(\r?\n)", "server-ip=" + host.config.get().getSection("Settings").getRawString("Server-Bind") + "$1");
        is.close();
        file.delete();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(content);
        writer.close();
    }
}
