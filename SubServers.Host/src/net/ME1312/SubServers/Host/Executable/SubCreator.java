package net.ME1312.SubServers.Host.Executable;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.Exception.InvalidTemplateException;
import net.ME1312.SubServers.Host.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Host.Library.NamedContainer;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.ExHost;
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
import java.util.regex.Pattern;

/**
 * Internal SubCreator Class
 */
public class SubCreator {
    private ExHost host;
    private TreeMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>> thread;

    public static class ServerTemplate {
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
            if (Util.isNull(name, enabled, directory, build, options)) throw new NullPointerException();
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
    }
    public enum ServerType {
        SPIGOT,
        VANILLA,
        SPONGE,
        CUSTOM;

        @Override
        public String toString() {
            return super.toString().substring(0, 1).toUpperCase()+super.toString().substring(1).toLowerCase();
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

    private YAMLSection build(NamedContainer<SubLogger, Process> thread, File dir, String name, ServerTemplate template, Version version, UUID address, List<ServerTemplate> history) throws SubCreatorException {
        YAMLSection server = new YAMLSection();
        boolean error = false;
        if (history.contains(template)) throw new IllegalStateException("Template Import loop detected");
        history.add(template);
        for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
            if (host.templates.keySet().contains(other.toLowerCase())) {
                YAMLSection config = build(thread, dir, other, host.templates.get(other.toLowerCase()), version, address, history);
                if (config == null) {
                    throw new SubCreatorException();
                } else {
                    server.setAll(config);
                }
            } else {
                thread.name().logger.warn.println("Skipping missing template: " + other);
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Skipping missing template: " + other));
            }
        }
        server.setAll(template.getConfigOptions());
        try {
            thread.name().logger.info.println("Loading Template: " + template.getDisplayName());
            host.subdata.sendPacket(new PacketOutExLogMessage(address, "Loading Template: " + template.getDisplayName()));
            Util.copyDirectory(template.getDirectory(), dir);
            if (template.getType() == ServerType.SPONGE) {
                thread.name().logger.info.println("Searching Versions...");
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Searching Versions..."));
                Document spongexml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://files.minecraftforge.net/maven/org/spongepowered/spongeforge/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));
                Document forgexml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                NodeList spnodeList = spongexml.getElementsByTagName("version");
                Version spversion = null;
                for (int i = 0; i < spnodeList.getLength(); i++) {
                    Node node = spnodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        if (node.getTextContent().startsWith(version.toString() + '-') && (spversion == null || new Version(node.getTextContent()).compareTo(spversion) >= 0)) {
                            spversion = new Version(node.getTextContent());
                        }
                    }
                }
                if (spversion == null)
                    throw new InvalidServerException("Cannot find Sponge version for Minecraft " + version.toString());
                thread.name().logger.info.println("Found \"spongeforge-" + spversion.toString() + '"');
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"spongeforge-" + spversion.toString() + '"'));

                NodeList mcfnodeList = forgexml.getElementsByTagName("version");
                Version mcfversion = null;
                for (int i = 0; i < mcfnodeList.getLength(); i++) {
                    Node node = mcfnodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        if (node.getTextContent().contains(spversion.toString().split("\\-")[1]) && (mcfversion == null || new Version(node.getTextContent()).compareTo(mcfversion) >= 0)) {
                            mcfversion = new Version(node.getTextContent());
                        }
                    }
                }
                if (mcfversion == null)
                    throw new InvalidServerException("Cannot find Forge version for Sponge " + spversion.toString());
                thread.name().logger.info.println("Found \"forge-" + mcfversion.toString() + '"');
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"forge-" + mcfversion.toString() + '"'));

                version = new Version(mcfversion.toString() + " " + spversion.toString());
            }
        } catch (Exception e) {
            thread.name().logger.error.println(e);
        }

        if (template.getBuildOptions().contains("Shell-Location")) {
            String gitBash = host.host.getRawString("Git-Bash") + ((host.host.getRawString("Git-Bash").endsWith(File.separator)) ? "" : File.separator) + "bin" + File.separatorChar + "bash.exe";
            File cache;
            if (template.getBuildOptions().getBoolean("Use-Cache", true)) {
                cache = new UniversalFile(host.dir, "Cache:Templates:" + template.getName());
                cache.mkdirs();
            } else {
                cache = null;
            }
            if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) && template.getBuildOptions().contains("Permission")) {
                try {
                    Process process = Runtime.getRuntime().exec("chmod " + template.getBuildOptions().getRawString("Permission") + ' ' + template.getBuildOptions().getRawString("Shell-Location"), null, dir);
                    Thread.sleep(500);
                    if (process.exitValue() != 0) {
                        thread.name().logger.info.println("Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location")));
                    }
                } catch (Exception e) {
                    thread.name().logger.info.println("Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    host.subdata.sendPacket(new PacketOutExLogMessage(address, "Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location")));
                    thread.name().logger.error.println(e);
                }
            }

            try {
                thread.name().logger.info.println("Launching " + template.getBuildOptions().getRawString("Shell-Location"));
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Launching " + template.getBuildOptions().getRawString("Shell-Location")));
                thread.set(Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)?"cmd.exe /c \"\"" + gitBash + "\" --login -i -c \"bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + ' ' + ((cache == null)?':':cache.toString().replace('\\', '/').replace(" ", "\\ ")) + "\"\"":("bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + ' ' + ((cache == null)?':':cache.toString().replace(" ", "\\ "))), null, dir));
                thread.name().file = new File(dir, "SubCreator-" + template.getName() + "-" + version.toString().replace(" ", "@") + ".log");
                thread.name().process = thread.get();
                thread.name().start();

                thread.get().waitFor();
                Thread.sleep(500);

                if (thread.get().exitValue() != 0) error = true;
            } catch (InterruptedException e) {
                error = true;
            } catch (Exception e) {
                error = true;
                thread.name().logger.error.println(e);
            }

            if (cache != null) {
                if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                cache = new UniversalFile(host.dir, "Cache:Templates");
                if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
                cache = new UniversalFile(host.dir, "Cache");
                if (cache.isDirectory() && cache.listFiles().length == 0) cache.delete();
            }
        }

        new UniversalFile(dir, "template.yml").delete();
        if (error) throw new SubCreatorException();
        return server;
    }

    private void run(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
        NamedContainer<SubLogger, Process> thread = this.thread.get(name.toLowerCase()).get();
        UniversalFile dir = new UniversalFile(new File(host.host.getRawString("Directory")), name);
        dir.mkdirs();
        YAMLSection server;
        try {
            server = build(thread, dir, name, template, version, address, new LinkedList<>());
            generateProperties(dir, port);
            generateClient(dir, template.getType(), name);
        } catch (SubCreatorException e) {
            server = null;
        } catch (Exception e) {
            server = null;
            thread.name().logger.error.println(e);
        }

        if (server != null) {
            host.subdata.sendPacket(new PacketExCreateServer(0, "Created Server Successfully", template.getConfigOptions(), id));
        } else {
            thread.name().logger.info.println("Couldn't build the server jar. Check the SubCreator logs for more detail.");
            host.subdata.sendPacket(new PacketExCreateServer(-1, "Couldn't build the server jar. Check the SubCreator logs for more detail.", template.getConfigOptions(), id));
        }
        this.thread.remove(name.toLowerCase());
    }

    public boolean create(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
        if (Util.isNull(name, template, version, port, address)) throw new NullPointerException();
        NamedContainer<Thread, NamedContainer<SubLogger, Process>> run = new NamedContainer<Thread, NamedContainer<SubLogger, Process>>(new Thread(() -> SubCreator.this.run(name, template, version, port, address, id)), new NamedContainer<SubLogger, Process>(new SubLogger(null, this, name + File.separator + "Creator", address, new Container<Boolean>(true), null), null));
        this.thread.put(name.toLowerCase(), run);
        run.name().start();
        return true;
    }

    public void terminate() {
        HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            terminate(i);
        }
    }

    public void terminate(String name) {
        if (this.thread.get(name).get().get() != null && this.thread.get(name).get().get().isAlive()) {
            this.thread.get(name).get().get().destroyForcibly();
        } else if (this.thread.get(name).name() != null && this.thread.get(name).name().isAlive()) {
            this.thread.get(name).name().interrupt();
            this.thread.remove(name);
        }
    }

    public void waitFor() throws InterruptedException {
        HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            waitFor(i);
        }
    }

    public void waitFor(String name) throws InterruptedException {
        while (this.thread.get(name).name() != null && this.thread.get(name).name().isAlive()) {
            Thread.sleep(250);
        }
    }

    public List<SubLogger> getLogger() {
        List<SubLogger> loggers = new ArrayList<SubLogger>();
        HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<SubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    public SubLogger getLogger(String name) {
        return this.thread.get(name).get().name();
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        if (new UniversalFile(dir, "subservers.client").exists()) {
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/client.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.SPONGE) {
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
        String content = Util.readAll(new BufferedReader(new InputStreamReader(is))).replace("server-port=", "server-port=" + port).replace("server-ip=", "server-ip=" + host.config.get().getSection("Settings").getRawString("Server-Bind"));
        is.close();
        file.delete();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(content);
        writer.close();
    }
}
