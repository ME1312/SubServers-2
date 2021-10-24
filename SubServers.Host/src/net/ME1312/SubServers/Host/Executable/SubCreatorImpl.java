package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Directories;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Map.ObjectMapValue;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Common.Network.API.SubCreator;
import net.ME1312.SubServers.Client.Common.Network.API.SubCreator.ServerType;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.Exception.InvalidTemplateException;
import net.ME1312.SubServers.Host.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Host.Library.ReplacementScanner;
import net.ME1312.SubServers.Host.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Host.SubAPI;

import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;

/**
 * Internal SubCreator Class
 */
public class SubCreatorImpl {
    private ExHost host;
    private TreeMap<String, CreatorTask> thread;

    public static class ServerTemplate extends SubCreator.ServerTemplate {
        private final boolean dynamic;
        private String name;
        private String nick = null;
        private boolean enabled;
        private boolean internal;
        private String icon;
        private File directory;
        private ServerType type;
        private ObjectMap<String> build;
        private ObjectMap<String> options;

        /**
         * Create a SubCreator Template
         *
         * @param name Template Name
         * @param enabled Template Enabled Status
         * @param icon Template Item Icon Name
         * @param directory Template Directory
         * @param build Build Options
         * @param options Configuration Options
         */
        public ServerTemplate(String name, boolean enabled, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options) {
            this(name, enabled, false, icon, directory, build, options, true);
        }

        private ServerTemplate(String name, boolean enabled, boolean internal, String icon, File directory, ObjectMap<String> build, ObjectMap<String> options, boolean dynamic) {
            super(toRaw(name, enabled, icon, directory, build, options));
            if (name.contains(" ")) throw new InvalidTemplateException("Template names cannot have spaces: " + name);
            this.name = name;
            this.enabled = enabled;
            this.internal = internal;
            this.icon = icon;
            this.directory = directory;
            this.type = (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM;
            this.build = build;
            this.options = options;
            this.dynamic = dynamic;
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
         * Get if this Template is for Internal use only
         *
         * @return Internal Status
         */
        public boolean isInternal() {
            return internal;
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
         * Get whether this Template was generated by a SubCreator instance
         *
         * @return Custom Status
         */
        public boolean isDynamic() {
            return dynamic;
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
            Util.nullpo(name, enabled, directory, build, options);
            ObjectMap<String> tinfo = new ObjectMap<String>();
            tinfo.set("enabled", enabled);
            tinfo.set("name", name);
            tinfo.set("display", name);
            tinfo.set("icon", icon);
            tinfo.set("type", (build.contains("Server-Type"))?ServerType.valueOf(build.getRawString("Server-Type").toUpperCase()):ServerType.CUSTOM);
            tinfo.set("version-req", build.getBoolean("Require-Version", false));
            tinfo.set("can-update", build.getBoolean("Can-Update", false));
            return tinfo;
        }
    }

    /**
     * Loads Template Metadata
     *
     * @param remote Loads from the Remote Templates directory when true
     */
    public void load(boolean remote) {
        HashMap<String, ServerTemplate> templates = (remote)?host.templatesR:host.templates;
        File dir = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), ((remote)?"Cache/Remote/":"") + "Templates");
        templates.clear();
        if (dir.exists()) for (File file : dir.listFiles()) {
                try {
                    if (file.isDirectory() && !file.getName().endsWith(".x")) {
                        ObjectMap<String> config = (new File(file, "template.yml").exists())?new YAMLConfig(new File(file, "template.yml")).get().getMap("Template", new ObjectMap<String>()):new ObjectMap<String>();
                        ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getBoolean("Internal", false), config.getRawString("Icon", "::NULL::"), file, config.getMap("Build", new ObjectMap<String>()), config.getMap("Settings", new ObjectMap<String>()), false);
                        templates.put(file.getName().toLowerCase(), template);
                        if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                    }
                } catch (Exception e) {
                    host.log.error.println("Couldn't load template: " + file.getName());
                    host.log.error.println(e);
                }
            }
    }

    private class CreatorTask extends Thread {
        private final HashMap<String, ServerTemplate> templates;
        private final SubServerImpl update;
        private final UUID player;
        private final String name;
        private final ServerTemplate template;
        private final Version version;
        private final int port;
        private final Boolean mode;
        private final UUID address;
        private final UUID tracker;
        private final SubLoggerImpl log;
        private final HashMap<String, String> replacements;
        private Process process;

        private CreatorTask(UUID player, String name, ServerTemplate template, Version version, int port, Boolean mode, UUID address, UUID tracker) {
            super(SubAPI.getInstance().getAppInfo().getName() + "::SubCreator_Process_Handler(" + name + ')');
            this.templates = new HashMap<String, ServerTemplate>();
            this.update = host.servers.getOrDefault(name.toLowerCase(), null);
            this.player = player;
            this.name = name;
            this.template = template;
            this.version = version;
            this.port = port;
            this.mode = mode;
            this.log = new SubLoggerImpl(null, this, name + File.separator + ((update == null)?"Creator":"Updater"), address, new Container<Boolean>(true), null);
            this.replacements = new HashMap<String, String>();
            this.address = address;
            this.tracker = tracker;

            templates.putAll(host.templatesR);
            templates.putAll(host.templates);
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
                                    log.logger.warn.println("Skipping template that's already loaded: " + other);
                                }
                            } else {
                                log.logger.warn.println("Skipping template that cannot be run in update mode: " + other);
                            }
                        } else {
                            log.logger.warn.println("Skipping template that requires extra versioning information: " + other);
                        }
                    } else {
                        log.logger.warn.println("Skipping disabled template: " + other);
                    }
                } else {
                    log.logger.warn.println("Skipping missing template: " + other);
                }
            }
            history.add(template);
            stack.remove(template);
            server.setAll(template.getConfigOptions());
            try {
                log.logger.info.println("Loading" + ((template.isDynamic())?" Dynamic":"") + " Template: " + template.getDisplayName());
                updateDirectory(template.getDirectory(), dir, template.getBuildOptions().getBoolean("Update-Files", false));

                for (ObjectMapValue<String> replacement : template.getBuildOptions().getMap("Replacements", new ObjectMap<>()).getValues()) if (!replacement.isNull()) {
                    replacements.put(replacement.getHandle().toLowerCase().replace('-', '_').replace(' ', '_'), replacement.asRawString());
                }

                var.putAll(replacements);
                var.put("java", System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
                var.put("mode", (update == null)? "CREATE" : ((mode)?"UPDATE":"SWITCH"));
                if (player != null) var.put("player", player.toString().toUpperCase());
                else var.remove("player");
                var.put("name", name);
                var.put("host", SubAPI.getInstance().getName());
                var.put("template", template.getName());
                var.put("type", template.getType().toString().toUpperCase());
                if (version != null) var.put("version", version.toString());
                else var.remove("version");
                var.put("address", getAddress());
                var.put("port", Integer.toString(port));
                switch (template.getType()) {
                    case SPONGE:
                    case FORGE:
                        if (version != null) {
                            log.logger.info.println("Searching Versions...");
                            YAMLSection spversionmanifest = new YAMLSection(new JSONObject("{\"versions\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://dl-api.spongepowered.org/v1/org.spongepowered/sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "/downloads?type=stable&minecraft=" + version).openStream(), Charset.forName("UTF-8")))) + '}'));

                            ObjectMap<String> spprofile = null;
                            Version spversion = null;
                            for (ObjectMap<String> profile : spversionmanifest.getMapList("versions")) {
                                if (profile.getMap("dependencies").getRawString("minecraft").equalsIgnoreCase(version.toString()) && (spversion == null || new Version(profile.getRawString("version")).compareTo(spversion) >= 0)) {
                                    spprofile = profile;
                                    spversion = new Version(profile.getRawString("version"));
                                }
                            }
                            if (spversion == null) throw new InvalidServerException("Cannot find Sponge version for Minecraft " + version.toString());
                            log.logger.info.println("Found \"sponge" + ((template.getType() == ServerType.FORGE)?"forge":"vanilla") + "-" + spversion.toString() + '"');

                            if (template.getType() == ServerType.FORGE) {
                                Version mcfversion = new Version(((spprofile.getMap("dependencies").getRawString("forge").contains("-"))?"":spprofile.getMap("dependencies").getRawString("minecraft") + '-') + spprofile.getMap("dependencies").getRawString("forge"));
                                log.logger.info.println("Found \"forge-" + mcfversion.toString() + '"');

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
                File cache = null;
                if (template.getBuildOptions().getBoolean("Use-Cache", true)) {
                    cache = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache/Templates/" + template.getName());
                    cache.mkdirs();
                    var.put("cache", cache.getAbsolutePath());
                }
                var.put("source", dir.getAbsolutePath());

                try {
                    log.logger.info.println("Launching Build Script...");
                    ProcessBuilder pb = new ProcessBuilder().command(Executable.parse(host.host.getRawString("Git-Bash"), template.getBuildOptions().getRawString("Executable"))).directory(dir);
                    pb.environment().putAll(var);
                    log.file = new File(dir, "SubCreator-" + template.getName() + "-" + ((version != null)?"-"+version.toString():"") + ".log");
                    process = pb.start();
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
                    cache = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache/Templates");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                    cache = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Cache");
                    if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                }
            }

            new File(dir, "template.yml").delete();
            if (error) throw new SubCreatorException();
            return server;
        }

        @SuppressWarnings("unchecked")
        public void run() {
            Runnable declaration = () -> {
                replacements.put("player", (player == null)?"":player.toString());
                replacements.put("name", name);
                replacements.put("host", SubAPI.getInstance().getName());
                replacements.put("template", template.getName());
                replacements.put("type", template.getType().toString());
                replacements.put("version", (version != null)?version.toString():"");
                replacements.put("address", getAddress());
                replacements.put("port", Integer.toString(port));
            };
            declaration.run();
            File dir = (update != null)?new File(update.getFullPath()):new File(host.host.getRawString("Directory"),
                    (template.getConfigOptions().contains("Directory"))?new ReplacementScanner(replacements).replace(template.getConfigOptions().getRawString("Directory")).toString():name);

            ObjectMap<String> config;
            try {
                log.init();
                config = build(dir, template, new LinkedList<>(), new LinkedList<>());
            } catch (SubCreatorException e) {
                config = null;
            } catch (Exception e) {
                config = null;
                log.logger.error.println(e);
            } finally {
                log.destroy();
            }

            declaration.run();
            ReplacementScanner replacements = new ReplacementScanner(this.replacements);
            if (config != null) {
                try {
                    if (template.getBuildOptions().getBoolean("Install-Client", true)) generateClient(dir, template.getType(), name);

                    LinkedList<String> masks = new LinkedList<>();
                    masks.add("/server.properties");
                    masks.addAll(template.getBuildOptions().getRawStringList("Replace", Collections.emptyList()));
                    replacements.replace(dir, masks.toArray(new String[0]));
                } catch (Exception e) {
                    config = null;
                    e.printStackTrace();
                }
            }

            if (config != null) {
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExCreateServer(0, null, (Map<String, ?>) replacements.replace(config.get()), tracker));
            } else {
                log.logger.info.println("Couldn't build the server jar. Check the SubCreator logs for more detail.");
                ((SubDataClient) SubAPI.getInstance().getSubDataNetwork()[0]).sendPacket(new PacketExCreateServer(-1, "Couldn't build the server jar. Check the SubCreator logs for more detail.", tracker));
            }
            SubCreatorImpl.this.thread.remove(name.toLowerCase());
        }
    }

    /**
     * Creates a SubCreator Instance
     *
     * @param host SubServers.Host
     */
    public SubCreatorImpl(ExHost host) {
        Util.nullpo(host);
        this.host = host;
        this.thread = new TreeMap<>();
    }

    public boolean create(UUID player, String name, ServerTemplate template, Version version, int port, Boolean mode, UUID address, UUID tracker) {
        Util.nullpo(name, template, port, address);
        CreatorTask task = new CreatorTask(player, name, template, version, port, mode, address, tracker);
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

    public List<SubLoggerImpl> getLoggers() {
        List<SubLoggerImpl> loggers = new ArrayList<SubLoggerImpl>();
        HashMap<String, CreatorTask> temp = new HashMap<String, CreatorTask>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    public SubLoggerImpl getLogger(String name) {
        return this.thread.get(name).log;
    }

    private static Pair<YAMLSection, String> address = null;
    private String getAddress() {
        if (address == null || host.config.get() != address.key()) {
            address = new ContainedPair<>(host.config.get(), host.config.get().getMap("Settings").getRawString("Server-Bind"));
        }
        return address.value();
    }

    private static Pair<YAMLSection, Map<String, Object>> subdata = null;
    private Map<String, Object> getSubData() {
        if (subdata == null || host.config.get() != subdata.key()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("Address", host.config.get().getMap("Settings").getMap("SubData").getRawString("Address"));
            if (host.config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) map.put("Password", host.config.get().getMap("Settings").getMap("SubData").getRawString("Password"));
            subdata = new ContainedPair<>(host.config.get(), map);
        }
        return subdata.value();
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        boolean installed = false;
        if (type == ServerType.SPIGOT) {
            installed = true;
            if (!new File(dir, "plugins").exists()) new File(dir, "plugins").mkdirs();
            if (!new File(dir, "plugins/SubServers.Client.jar").exists())
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new File(dir, "plugins/SubServers.Client.jar").getPath());
        } else if (type == ServerType.FORGE || type == ServerType.SPONGE) {
            installed = true;
            if (!new File(dir, "mods").exists()) new File(dir, "mods").mkdirs();
            if (!new File(dir, "mods/SubServers.Client.jar").exists())
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new File(dir, "mods/SubServers.Client.jar").getPath());
        }

        if (installed) {
            YAMLSection config = new YAMLSection();
            FileWriter writer = new FileWriter(new File(dir, "subdata.json"), false);
            config.setAll(getSubData());
            writer.write(config.toJSON().toString());
            writer.close();

            if (!new File(dir, "subdata.rsa.key").exists() && new File("subdata.rsa.key").exists()) {
                Files.copy(new File("subdata.rsa.key").toPath(), new File(dir, "subdata.rsa.key").toPath());
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
