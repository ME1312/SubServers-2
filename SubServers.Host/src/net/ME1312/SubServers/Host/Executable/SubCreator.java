package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.Exception.InvalidTemplateException;
import net.ME1312.SubServers.Host.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Host.Network.API.SubCreator.ServerType;
import net.ME1312.SubServers.Host.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
        private ObjectMap<String> build;
        private ObjectMap<String> options;

        /**
         * Create a SubCreator Template
         *
         * @param name Template Name
         * @param directory Template Directory
         * @param build Build Options
         * @param options Configuration Options
         */
        public ServerTemplate(String name, boolean enabled, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options) {
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
         * Get whether this Template requires the Version argument
         *
         * @return Version Requirement
         */
        public boolean requiresVersion() {
            return getBuildOptions().getBoolean("Require-Version", false);
        }

        /**
         * Get whether this Template can be used to update it's servers
         *
         * @return Updatable Status
         */
        public boolean canUpdate() {
            return getBuildOptions().getBoolean("Can-Update", false);
        }

        /**
         * Get the Build Options for this Template
         *
         * @return Build Options
         */
        public ObjectMap<String> getBuildOptions() {
            return build;
        }

        /**
         * Get the Configuration Options for this Template
         *
         * @return Configuration Options
         */
        public ObjectMap<String> getConfigOptions() {
            return options;
        }

        private static ObjectMap<String> toRaw(String name, boolean enabled, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options) {
            if (Util.isNull(name, enabled, directory, build, options)) throw new NullPointerException();
            ObjectMap<String> tinfo = new ObjectMap<String>();
            tinfo.set("enabled", enabled);
            tinfo.set("name", name);
            tinfo.set("display", name);
            tinfo.set("icon", icon);
            tinfo.set("type", (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM);
            return tinfo;
        }
    }

    private class CreatorTask extends Thread {
        private final SubServer update;
        private final String name;
        private final ServerTemplate template;
        private final Version version;
        private final int port;
        private final File dir;
        private final UUID address;
        private final UUID tracker;
        private final SubLogger log;
        private Process process;

        private CreatorTask(String name, ServerTemplate template, Version version, int port, String dir, UUID address, UUID tracker) {
            super(SubAPI.getInstance().getAppInfo().getName() + "::SubCreator_Process_Handler(" + name + ')');
            this.update = host.servers.getOrDefault(name.toLowerCase(), null);
            this.name = name;
            this.template = template;
            this.version = version;
            this.port = port;
            this.dir = new File(host.host.getRawString("Directory"), dir.replace("$address$", host.config.get().getMap("Settings").getRawString("Server-Bind")));
            this.log = new SubLogger(null, this, name + File.separator + ((update == null)?"Creator":"Updater"), address, new Container<Boolean>(true), null);
            this.address = address;
            this.tracker = tracker;
        }

        private ObjectMap<String> build(File dir, ServerTemplate template, List<ServerTemplate> history) throws SubCreatorException {
            ObjectMap<String> server = new ObjectMap<String>();
            Version version = this.version;
            HashMap<String, String> var = new HashMap<String, String>();
            boolean error = false;
            if (history.contains(template)) throw new IllegalStateException("Template Import loop detected");
            history.add(template);
            for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
                if (host.templates.keySet().contains(other.toLowerCase())) {
                    if (host.templates.get(other.toLowerCase()).isEnabled()) {
                        if (version != null || !host.templates.get(other.toLowerCase()).requiresVersion()) {
                            if (update == null || host.templates.get(other.toLowerCase()).canUpdate()) {
                                ObjectMap<String> config = build(dir, host.templates.get(other.toLowerCase()), history);
                                if (config == null) {
                                    throw new SubCreatorException();
                                } else {
                                    server.setAll(config);
                                }
                            } else {
                                log.logger.warn.println("Skipping template that cannot be run in update mode: " + other);
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Skipping template that cannot be run in update mode: " + other));
                            }
                        } else {
                            log.logger.warn.println("Skipping template that requires extra versioning: " + other);
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Skipping template that requires extra versioning: " + other));
                        }
                    } else {
                        log.logger.warn.println("Skipping disabled template: " + other);
                        ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Skipping disabled template: " + other));
                    }
                } else {
                    log.logger.warn.println("Skipping missing template: " + other);
                    ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Skipping missing template: " + other));
                }
            }
            server.setAll(template.getConfigOptions());
            try {
                log.logger.info.println("Loading Template: " + template.getDisplayName());
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Loading Template: " + template.getDisplayName()));
                Util.copyDirectory(template.getDirectory(), dir);
                var.put("mode", (update == null)?"CREATE":"UPDATE");
                var.put("name", name);
                if (SubAPI.getInstance().getSubDataNetwork()[0] != null) var.put("host", SubAPI.getInstance().getName());
                var.put("template", template.getName());
                var.put("type", template.getType().toString().toUpperCase());
                if (version != null) var.put("version", version.toString());
                var.put("address", host.config.get().getMap("Settings").getRawString("Server-Bind"));
                var.put("port", Integer.toString(port));
                switch (template.getType()) {
                    case SPONGE:
                    case FORGE:
                        if (version != null) {
                            log.logger.info.println("Searching Versions...");
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Searching Versions..."));
                            YAMLSection spversionmanifest = new YAMLSection(new JSONObject("{\"versions\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://dl-api.spongepowered.org/v1/org.spongepowered/sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "/downloads?type=stable&minecraft=" + version).openStream(), Charset.forName("UTF-8")))) + '}'));

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
                            log.logger.info.println("Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"');
                            ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"'));

                            if (template.getType() == ServerType.FORGE) {
                                Version mcfversion = new Version(((spprofile.getMap("dependencies").getRawString("forge").contains("-"))?"":spprofile.getMap("dependencies").getRawString("minecraft") + '-') + spprofile.getMap("dependencies").getRawString("forge"));
                                log.logger.info.println("Found \"forge-" + mcfversion.toString() + '"');
                                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Found \"forge-" + mcfversion.toString() + '"'));

                                var.put("mcf_version", mcfversion.toString());
                            }
                            var.put("sp_version", spversion.toString());
                        }
                        break;
                }
            } catch (Exception e) {
                log.logger.error.println(e);
            }

            if (template.getBuildOptions().contains("Executable")) {
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
                    ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketOutExLogMessage(address, "Launching Build Script..."));
                    ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.host.getRawString("Git-Bash"), template.getBuildOptions().getRawString("Executable"))).directory(dir);
                    pb.environment().putAll(var);
                    process = pb.start();
                    log.file = new File(dir, "SubCreator-" + template.getName() + "-" + ((version != null)?"-"+version.toString():"") + ".log");
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
            dir.mkdirs();
            ObjectMap<String> server;
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
            ObjectMap<String> config = template.getConfigOptions().clone();
            if (server != null) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExCreateServer(0, null, config, host.config.get().getMap("Settings").getRawString("Server-Bind"), tracker));
            } else {
                log.logger.info.println("Couldn't build the server jar. Check the SubCreator logs for more detail.");
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExCreateServer(-1, "Couldn't build the server jar. Check the SubCreator logs for more detail.", tracker));
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

    public boolean create(String name, ServerTemplate template, Version version, int port, String dir, UUID address, UUID tracker) {
        if (Util.isNull(name, template, port, dir, address)) throw new NullPointerException();
        CreatorTask task = new CreatorTask(name, template, version, port, dir, address, tracker);
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
            Files.delete(new UniversalFile(dir, "subservers.client").toPath());
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                if (!new UniversalFile(dir, "plugins:SubServers.Client.jar").exists())
                    Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.FORGE || type == ServerType.SPONGE) {
                if (!new UniversalFile(dir, "mods").exists()) new UniversalFile(dir, "mods").mkdirs();
                if (!new UniversalFile(dir, "mods:SubServers.Client.jar").exists())
                    Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(dir, "mods:SubServers.Client.jar").getPath());
            }
            JSONObject config = new JSONObject();
            FileWriter writer = new FileWriter(new UniversalFile(dir, "subdata.json"), false);
            config.put("Name", name);
            config.put("Address", host.config.get().getMap("Settings").getMap("SubData").getRawString("Address"));
            if (host.config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) config.put("Password", host.config.get().getMap("Settings").getMap("SubData").getRawString("Password"));
            config.write(writer);
            writer.close();

            if (!new UniversalFile(dir, "subdata.rsa.key").exists() && new UniversalFile("subdata.rsa.key").exists()) {
                Files.copy(new UniversalFile("subdata.rsa.key").toPath(), new UniversalFile(dir, "subdata.rsa.key").toPath());
            }
        }
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
        if (!file.exists()) file.createNewFile();
        FileInputStream is = new FileInputStream(file);
        String content = Util.readAll(new BufferedReader(new InputStreamReader(is))).replaceAll("server-port=.*(\r?\n)", "server-port=" + port + "$1").replaceAll("server-ip=.*(\r?\n)", "server-ip=" + host.config.get().getMap("Settings").getRawString("Server-Bind") + "$1");
        is.close();
        file.delete();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(content);
        writer.close();
    }
}
