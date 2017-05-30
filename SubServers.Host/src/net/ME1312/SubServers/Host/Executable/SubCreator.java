package net.ME1312.SubServers.Host.Executable;

import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.Exception.InvalidTemplateException;
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
import java.util.UUID;

/**
 * Internal SubCreator Class
 */
public class SubCreator {
    private ExHost host;
    private SubLogger logger;
    private Process process = null;
    private Thread thread = null;

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
        this.logger = new SubLogger(null, this, "SubCreator", null, new Container<Boolean>(false), null);
    }

    private void run(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
        UniversalFile dir = new UniversalFile(new File(host.host.getRawString("Directory")), name);
        dir.mkdirs();

        logger.logger.info.println("Generating Server Files...");
        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Generating Server Files..."));
        try {
            Util.copyDirectory(template.getDirectory(), dir);
            generateProperties(dir, port);
            generateClient(dir, template.getType(), name);

            if (template.getType() == ServerType.SPONGE) {
                logger.logger.info.println("Searching Versions...");
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
                    throw new InvalidServerException("Cannot find sponge version for Minecraft " + version.toString());
                logger.logger.info.println("Found \"spongeforge-" + spversion.toString() + '"');
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
                    throw new InvalidServerException("Cannot find forge version for Sponge " + spversion.toString());
                logger.logger.info.println("Found \"forge-" + mcfversion.toString() + '"');
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"forge-" + mcfversion.toString() + '"'));

                version = new Version(mcfversion.toString() + " " + spversion.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean error = false;
        if (template.getBuildOptions().getKeys().size() > 0) {
            File gitBash = new File(host.host.getRawString("Git-Bash"), "bin" + File.separatorChar + "bash.exe");
            if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) && template.getBuildOptions().contains("Permission")) {
                try {
                    Process process = Runtime.getRuntime().exec("chmod " + template.getBuildOptions().getRawString("Permission") + ' ' + template.getBuildOptions().getRawString("Shell-Location"), null, dir);
                    Thread.sleep(500);
                    if (process.exitValue() != 0) {
                        logger.logger.info.println("Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location")));
                    }
                } catch (Exception e) {
                    logger.logger.info.println("Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    host.subdata.sendPacket(new PacketOutExLogMessage(address, "Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location")));
                    e.printStackTrace();
                }
            }

            try {
                logger.logger.info.println("/Creator > Launching " + template.getBuildOptions().getRawString("Shell-Location"));
                host.subdata.sendPacket(new PacketOutExLogMessage(address,"/Creator > Launching " + template.getBuildOptions().getRawString("Shell-Location")));
                process = Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)?"\"" + gitBash + "\" --login -i -c \"bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + '\"':("bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + " " + System.getProperty("user.home")), null, dir);
                logger.process = this.process;
                logger.log.set(true);
                logger.address = address;
                logger.file = new File(dir, "SubCreator-" + template.getType().toString() + "-" + version.toString().replace(" ", "@") + ".log");
                logger.start();

                process.waitFor();
                Thread.sleep(500);

                if (process.exitValue() != 0) error = true;
            } catch (Exception e) {
                error = true;
                e.printStackTrace();
            }
        }

        new UniversalFile(dir, "template.yml").delete();
        if (!error) {
            host.subdata.sendPacket(new PacketExCreateServer(process.exitValue(), "Created Server Successfully", template.getConfigOptions().toJSON(), id));
        } else {
            logger.logger.info.println("Couldn't build the server jar. See \"SubCreator-" + template.getType().toString() + "-" + version.toString().replace(" ", "@") + ".log\" for more details.");
            host.subdata.sendPacket(new PacketOutExLogMessage(address, "Couldn't build the server jar. See \"SubCreator-" + template.getType().toString() + "-" + version.toString().replace(" ", "@") + ".log\" for more details."));
        }
    }

    public boolean create(String name, ServerTemplate template, Version version, int port, UUID address, String id) {
        if (Util.isNull(name, template, version, port, address)) throw new NullPointerException();
        if (!isBusy()) {
            (thread = new Thread(() -> {
                SubCreator.this.run(name, template, version, port, address, id);
            })).start();
            return true;
        } else return false;
    }

    public void terminate() {
        if (process != null && this.process.isAlive()) {
            process.destroyForcibly();
        } else if (thread != null && this.thread.isAlive()) {
            thread.interrupt();
        }
    }

    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
            Thread.sleep(250);
        }
    }

    public SubLogger getLogger() {
        return logger;
    }

    public boolean isBusy() {
        return thread != null && thread.isAlive();
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        if (new UniversalFile(dir, "subservers.client").exists()) {
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/Client/spigot.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.SPONGE) {
                // TODO
                // if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "mods").mkdirs();
                // Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/Client/sponge.jar", new UniversalFile(dir, "mods:SubServers.Client.jar").getPath());
            }
            JSONObject config = new JSONObject(Util.readAll(new FileReader(new UniversalFile(dir, "subservers.client"))));
            FileWriter writer = new FileWriter(new UniversalFile(dir, "subservers.client"), false);
            config.put("Name", name);
            config.put("Address", host.config.get().getSection("Settings").getSection("SubData").getRawString("Address"));
            config.put("Password", host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"));
            config.write(writer);
            writer.close();
        }
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
        if (!file.exists()) file.createNewFile();
        String content = Util.readAll(new BufferedReader(new InputStreamReader(new FileInputStream(file)))).replace("server-port=", "server-port=" + port).replace("server-ip=", "server-ip=" + host.config.get().getSection("Settings").getRawString("Server-Bind"));
        file.delete();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(content);
        writer.close();
    }
    private void copyFolder(File source, File destination) {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
