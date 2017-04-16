package net.ME1312.SubServers.Host.Executable;

import net.ME1312.SubServers.Host.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Host.Library.Config.YAMLSection;
import net.ME1312.SubServers.Host.Library.Container;
import net.ME1312.SubServers.Host.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Host.Library.UniversalFile;
import net.ME1312.SubServers.Host.Library.Util;
import net.ME1312.SubServers.Host.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.Packet.PacketDownloadBuildScript;
import net.ME1312.SubServers.Host.Network.Packet.PacketExCreateServer;
import net.ME1312.SubServers.Host.Network.Packet.PacketOutExLogMessage;
import net.ME1312.SubServers.Host.ExHost;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.UUID;

/**
 * Internal SubCreator Class
 */
public class SubCreator {
    private ExHost host;
    private SubLogger logger;
    private Process process = null;
    private Thread thread = null;

    public enum ServerType {
        SPIGOT,
        VANILLA,
        SPONGE,;

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


    private void run(String name, ServerType type, Version version, int memory, int port, UUID address, String id) {
        Executable executable = null;
        UniversalFile dir = new UniversalFile(new File(host.host.getRawString("Directory")), name);
        dir.mkdirs();

        logger.logger.info.println("Generating Server Files...");
        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Generating Server Files..."));
        if (type == ServerType.SPIGOT) {
            executable = new Executable("java -Xmx" + memory + "M -Djline.terminal=jline.UnsupportedTerminal -Dcom.mojang.eula.agree=true -jar Spigot.jar");

            try {
                copyFolder(new UniversalFile(host.dir, "Templates:Spigot"), dir);
                generateProperties(dir, port);
                generateClient(dir, name, type);
            } catch (Exception e) {
                logger.logger.error.println(e);
            }

        } else if (type == ServerType.VANILLA) {
            executable = new Executable("java -Xmx" + memory + "M -jar Vanilla.jar nogui");

            try {
                copyFolder(new UniversalFile(host.dir, "Templates:Vanilla"), dir);
                generateEULA(dir);
                generateProperties(dir, port);
            } catch (IOException e) {
                logger.logger.error.println(e);
            }
        } else if (type == ServerType.SPONGE) {
            try {
                executable = new Executable("java -Xmx" + memory + "M -jar Forge.jar");

                copyFolder(new UniversalFile(host.dir, "" +
                        "Templates:Sponge"), dir);
                generateEULA(dir);
                generateProperties(dir, port);
                generateClient(dir, name, type);

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
                if (spversion == null) throw new InvalidServerException("Cannot find sponge version for Minecraft " + version.toString());
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
                if (mcfversion == null) throw new InvalidServerException("Cannot find forge version for Sponge " + spversion.toString());
                logger.logger.info.println("Found \"forge-" + mcfversion.toString() + '"');
                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Found \"forge-" + mcfversion.toString() + '"'));

                version = new Version(mcfversion.toString() + "::" + spversion.toString());
            } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {
                logger.logger.error.println(e);
            }
        }

        Version ver = version;
        Executable exec = executable;
        host.subdata.sendPacket(new PacketDownloadBuildScript(json -> {
            (thread = new Thread(() -> {
                try {

                    PrintWriter writer = new PrintWriter(new UniversalFile(dir, "build.sh"), "UTF-8");
                    Iterator<Object> i = json.getJSONArray("script").iterator();
                    while (i.hasNext()) {
                        String line = (String) i.next();
                        writer.println(line);
                    }
                    writer.close();

                    if (!(new File(dir, "build.sh").exists())) {
                        logger.logger.info.println("Problem copying build.sh");
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Problem copying build.sh"));
                    } else {
                        File gitBash = new File(host.host.getRawString("Git-Bash"), "bin" + File.separatorChar + "bash.exe");
                        if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)) {
                            Process process = Runtime.getRuntime().exec("chmod +x build.sh", null, dir);
                            try {
                                process.waitFor();
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                logger.logger.error.println(e);
                            }
                            if (process.exitValue() != 0) {
                                logger.logger.info.println("Problem Setting Executable Permissions.");
                                host.subdata.sendPacket(new PacketOutExLogMessage(address, "Problem Setting Executable Permissions."));
                            }
                        }

                        logger.logger.info.println("Launching build.sh");
                        host.subdata.sendPacket(new PacketOutExLogMessage(address, "Launching build.sh"));
                        this.process = Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) ?
                                "\"" + gitBash + "\" --login -i -c \"bash build.sh " + ver.toString() + " " + type.toString().toLowerCase() + "\""
                                : ("bash build.sh " + ver.toString() + " " + type.toString().toLowerCase() + " " + System.getProperty("user.home")), null, dir);
                        logger.process = this.process;
                        logger.log.set(true);
                        logger.address = address;
                        logger.file = new File(dir, "SubCreator-" + type.toString() + "-" + ver.toString().replace("::", "@") + ".log");
                        logger.start();
                        try {
                            this.process.waitFor();
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            logger.logger.error.println(e);
                        }

                        JSONObject info = new JSONObject();
                        info.put("dir", "." + File.separatorChar + name);
                        info.put("exec", exec.toString());
                        host.subdata.sendPacket(new PacketExCreateServer(process.exitValue(), (this.process.exitValue() == 0) ? "Created Server Successfully" : ("Couldn't build the server jar. See \"SubCreator-" + type.toString() + "-" + ver.toString().replace("::", "@") + ".log\" for more details."), info, id));
                        if (this.process.exitValue() != 0) {
                            logger.logger.info.println("Couldn't build the server jar. See \"SubCreator-" + type.toString() + "-" + ver.toString().replace("::", "@") + ".log\" for more details.");
                        }
                    }
                } catch (IOException e) {
                    host.log.error.println(e);
                    host.subdata.sendPacket(new PacketExCreateServer(-1, "An Exception occurred while running SubCreator. See the " + host.subdata.getName() + " console for more details.", null, id));
                }
            })).start();
        }));
    }

    public boolean create(String name, ServerType type, Version version, int memory, int port, UUID address, String id) {
        if (Util.isNull(name, type, version, memory, port, address)) throw new NullPointerException();
        if (!isBusy()) {
            (thread = new Thread(() -> {
                SubCreator.this.run(name, type, version, memory, port, address, id);
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

    private void generateClient(File dir, String name, ServerType type) throws IOException {
        if (type == ServerType.SPIGOT) {
            new UniversalFile(dir, "plugins:SubServers-Client-Bukkit").mkdirs();
            Util.copyFromJar(ExHost.class.getClassLoader(), "net/ME1312/SubServers/Host/Library/Files/bukkit.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            YAMLConfig config = new YAMLConfig(new UniversalFile(dir, "plugins:Subservers-Client-Bukkit:config.yml"));
            YAMLSection settings = new YAMLSection();
            settings.set("Version", "2.11.2a+");
            settings.set("Ingame-Access", true);
            settings.set("Use-Title-Messages", true);
            YAMLSection subdata = new YAMLSection();
            subdata.set("Name", name);
            subdata.set("Address", host.config.get().getSection("Settings").getSection("SubData").getRawString("Address"));
            subdata.set("Password", host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"));
            settings.set("SubData", subdata);
            config.get().set("Settings", settings);
            config.save();
        } else if (type == ServerType.SPONGE) {
            // TODO
        }
    }
    private void generateEULA(File dir) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(new File(dir, "eula.txt"), "UTF-8");

        writer.println("#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).");
        writer.println("#" + new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy").format(Calendar.getInstance().getTime()));
        writer.println("eula=true");

        writer.close();
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
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
