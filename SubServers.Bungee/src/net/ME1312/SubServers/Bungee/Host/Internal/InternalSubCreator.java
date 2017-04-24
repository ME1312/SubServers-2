package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubPlugin;
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
import java.util.UUID;

/**
 * Internal SubCreator Class
 */
public class InternalSubCreator extends SubCreator {
    private InternalHost host;
    private String gitBash;
    private InternalSubLogger logger;
    private Process process = null;
    private Thread thread = null;

    /**
     * Creates an Internal SubCreator
     *
     * @param host Host
     * @param gitBash Git Bash
     */
    public InternalSubCreator(InternalHost host, String gitBash) {
        if (Util.isNull(host, gitBash)) throw new NullPointerException();
        this.host = host;
        this.gitBash = gitBash;
        this.logger = new InternalSubLogger(null, this, host.getName() + "/Creator", new Container<Boolean>(false), null);
    }


    private void run(UUID player, String name, ServerType type, Version version, int memory, int port) {
        Executable exec = null;
        UniversalFile dir = new UniversalFile(new File(host.getPath()), name);
        dir.mkdirs();

        System.out.println(host.getName() + "/Creator > Generating Server Files...");
        if (type == ServerType.SPIGOT) {
            exec = new Executable("java -Xmx" + memory + "M -Djline.terminal=jline.UnsupportedTerminal -Dcom.mojang.eula.agree=true -jar Spigot.jar");

            try {
                copyFolder(new UniversalFile(host.plugin.dir, "SubServers:Templates:Spigot"), dir);
                generateProperties(dir, port);
                generateClient(dir, name, type);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (type == ServerType.VANILLA) {
            exec = new Executable("java -Xmx" + memory + "M -jar Vanilla.jar nogui");

            try {
                copyFolder(new UniversalFile(host.plugin.dir, "SubServers:Templates:Vanilla"), dir);
                generateEULA(dir);
                generateProperties(dir, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (type == ServerType.SPONGE) {
            try {
                exec = new Executable("java -Xmx" + memory + "M -jar Forge.jar");

                copyFolder(new UniversalFile(host.plugin.dir, "SubServers:Templates:Sponge"), dir);
                generateEULA(dir);
                generateProperties(dir, port);
                generateClient(dir, name, type);

                System.out.println(host.getName() + "/Creator > Searching Versions...");
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
                System.out.println(host.getName() + "/Creator > Found \"spongeforge-" + spversion.toString() + '"');

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
                System.out.println(host.getName() + "/Creator > Found \"forge-" + mcfversion.toString() + '"');

                version = new Version(mcfversion.toString() + "::" + spversion.toString());
            } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        try {
            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(new UniversalFile(host.plugin.dir, "SubServers:build.sh"));
                output = new FileOutputStream(new File(dir, "build.sh"));
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) > 0) {
                    output.write(buf, 0, bytesRead);
                }
            } finally {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
            }

            if (!(new File(dir, "build.sh").exists())) {
                System.out.println(host.getName() + "/Creator > Problem copying build.sh");
            } else {
                File gitBash = new File(this.gitBash, "bin" + File.separatorChar + "bash.exe");
                if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)) {
                    Process process = Runtime.getRuntime().exec("chmod +x build.sh", null, dir);
                    try {
                        process.waitFor();
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (process.exitValue() != 0) {
                        System.out.println(host.getName() + "/Creator > Problem Setting Executable Permissions.");
                    }
                }

                System.out.println(host.getName() + "/Creator > Launching build.sh");
                this.process = Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)?
                        "\"" + gitBash + "\" --login -i -c \"bash build.sh " + version.toString() + " " + type.toString().toLowerCase() + "\""
                        :("bash build.sh " + version.toString() + " " + type.toString().toLowerCase() + " " + System.getProperty("user.home")), null, dir);
                logger.process = this.process;
                logger.log.set(host.plugin.config.get().getSection("Settings").getBoolean("Log-Creator"));
                logger.file = new File(dir, "SubCreator-" + type.toString() + "-" + version.toString().replace("::", "@") + ".log");
                logger.start();
                try {
                    this.process.waitFor();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (this.process.exitValue() == 0) {
                    System.out.println(host.getName() + "/Creator > Saving...");
                    if (host.plugin.exServers.keySet().contains(name.toLowerCase())) host.plugin.exServers.remove(name.toLowerCase());
                    SubServer subserver = host.addSubServer(player, name, true, port, "Some SubServer", true, "." + File.separatorChar + name, exec, "stop", false, false, false, false, false);

                    YAMLSection server = new YAMLSection();
                    server.set("Enabled", true);
                    server.set("Host", host.getName());
                    server.set("Port", port);
                    server.set("Motd", "Some SubServer");
                    server.set("Log", true);
                    server.set("Directory", "." + File.separatorChar + name);
                    server.set("Executable", exec.toString());
                    server.set("Stop-Command", "stop");
                    server.set("Run-On-Launch", false);
                    server.set("Auto-Restart", false);
                    server.set("Hidden", false);
                    server.set("Restricted", false);
                    host.plugin.config.get().getSection("Servers").set(name, server);
                    host.plugin.config.save();

                    subserver.start(player);
                } else {
                    System.out.println(host.getName() + "/Creator > Couldn't build the server jar. See \"SubCreator-" + type.toString() + "-" + version.toString().replace("::", "@") + ".log\" for more details.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean create(UUID player, String name, ServerType type, Version version, int memory, int port) {
        if (Util.isNull(name, type, version, memory, port)) throw new NullPointerException();
        if (!isBusy()) {
            final SubCreateEvent event = new SubCreateEvent(player, host, name, type, version, memory, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                (thread = new Thread(() -> {
                        InternalSubCreator.this.run(player, name, event.getType(), event.getVersion(), event.getMemory(), port);
                    })).start();
                return true;
            } else return false;
        } else return false;
    }

    @Override
    public void terminate() {
        if (process != null && this.process.isAlive()) {
            process.destroyForcibly();
        } else if (thread != null && this.thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        while (thread != null && thread.isAlive()) {
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
        return thread != null && thread.isAlive();
    }

    private void generateClient(File dir, String name, ServerType type) throws IOException {
        if (type == ServerType.SPIGOT) {
            new UniversalFile(dir, "plugins:SubServers-Client-Bukkit").mkdirs();
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bukkit.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            YAMLConfig config = new YAMLConfig(new UniversalFile(dir, "plugins:Subservers-Client-Bukkit:config.yml"));
            YAMLSection settings = new YAMLSection();
            settings.set("Version", "2.11.2a+");
            settings.set("Ingame-Access", true);
            settings.set("Use-Title-Messages", true);
            YAMLSection subdata = new YAMLSection();
            subdata.set("Name", name);
            subdata.set("Address", host.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1").replace("0.0.0.0", "127.0.0.1"));
            subdata.set("Password", host.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password", ""));
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
        String content = Util.readAll(new BufferedReader(new InputStreamReader(new FileInputStream(file)))).replace("server-port=", "server-port=" + port).replace("server-ip=", "server-ip=" + host.getAddress().toString().substring(1));
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
