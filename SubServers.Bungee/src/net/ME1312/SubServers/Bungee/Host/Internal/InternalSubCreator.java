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
public class InternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
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

        if (new UniversalFile(host.plugin.dir, "SubServers:Templates").exists()) for (File file : new UniversalFile(host.plugin.dir, "SubServers:Templates").listFiles()) {
            try {
                if (file.isDirectory()) {
                    YAMLSection config = (new UniversalFile(file, "template.yml").exists())?new YAMLConfig(new UniversalFile(file, "template.yml")).get().getSection("Template", new YAMLSection()):new YAMLSection();
                    ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getSection("Build", new YAMLSection()), config.getSection("Settings", new YAMLSection()));
                    templates.put(file.getName().toLowerCase(), template);
                    if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                }
            } catch (Exception e) {
                System.out.println(host.getName() + "/Creator > Couldn't load template: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private void run(UUID player, String name, ServerTemplate template, Version version, int port) {
        UniversalFile dir = new UniversalFile(new File(host.getPath()), name);
        dir.mkdirs();

        System.out.println(host.getName() + "/Creator > Generating Server Files...");
        try {
            Util.copyDirectory(template.getDirectory(), dir);
            generateProperties(dir, port);
            generateClient(dir, template.getType(), name);

            if (template.getType() == ServerType.SPONGE) {
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
                if (spversion == null)
                    throw new InvalidServerException("Cannot find sponge version for Minecraft " + version.toString());
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
                if (mcfversion == null)
                    throw new InvalidServerException("Cannot find forge version for Sponge " + spversion.toString());
                System.out.println(host.getName() + "/Creator > Found \"forge-" + mcfversion.toString() + '"');

                version = new Version(mcfversion.toString() + " " + spversion.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean error = false;
        if (template.getBuildOptions().getKeys().size() > 0) {
            File gitBash = new File(this.gitBash, "bin" + File.separatorChar + "bash.exe");
            if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) && template.getBuildOptions().contains("Permission")) {
                try {
                    Process process = Runtime.getRuntime().exec("chmod " + template.getBuildOptions().getRawString("Permission") + ' ' + template.getBuildOptions().getRawString("Shell-Location"), null, dir);
                    Thread.sleep(500);
                    if (process.exitValue() != 0) {
                        System.out.println(host.getName() + "/Creator > Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    }
                } catch (Exception e) {
                    System.out.println(host.getName() + "/Creator > Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    e.printStackTrace();
                }
            }

            try {
                System.out.println(host.getName() + "/Creator > Launching " + template.getBuildOptions().getRawString("Shell-Location"));
                process = Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)?"\"" + gitBash + "\" --login -i -c \"bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + '\"':("bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + " " + System.getProperty("user.home")), null, dir);
                logger.process = this.process;
                logger.log.set(host.plugin.config.get().getSection("Settings").getBoolean("Log-Creator"));
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
            try {
                System.out.println(host.getName() + "/Creator > Saving...");
                if (host.plugin.exServers.keySet().contains(name.toLowerCase())) host.plugin.exServers.remove(name.toLowerCase());

                YAMLSection server = template.getConfigOptions().clone();
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
                if (!server.contains("Executable")) server.set("Executable", "java -Xmx1024M -jar " + template.getType().toString() + ".jar");
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(host.getName() + "/Creator > Couldn't build the server jar. See \"SubCreator-" + template.getType().toString() + "-" + version.toString().replace(" ", "@") + ".log\" for more details.");
        }
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, int port) {
        if (Util.isNull(name, template, version, port)) throw new NullPointerException();
        if (!isBusy() && template.isEnabled()) {
            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                (thread = new Thread(() -> {
                        InternalSubCreator.this.run(player, name, event.getTemplate(), event.getVersion(), port);
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

    @Override
    public Map<String, ServerTemplate> getTemplates() {
        return new TreeMap<String, ServerTemplate>(templates);
    }

    @Override
    public ServerTemplate getTemplate(String name) {
        if (Util.isNull(name)) throw new NullPointerException();
        return getTemplates().get(name.toLowerCase());
    }

    private void generateClient(File dir, ServerType type, String name) throws IOException {
        if (new UniversalFile(dir, "subservers.client").exists()) {
            if (type == ServerType.SPIGOT) {
                if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "plugins").mkdirs();
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/Client/spigot.jar", new UniversalFile(dir, "plugins:SubServers.Client.jar").getPath());
            } else if (type == ServerType.SPONGE) {
                // TODO
                // if (!new UniversalFile(dir, "plugins").exists()) new UniversalFile(dir, "mods").mkdirs();
                // Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/Client/sponge.jar", new UniversalFile(dir, "mods:SubServers.Client.jar").getPath());
            }
            JSONObject config = new JSONObject();
            FileWriter writer = new FileWriter(new UniversalFile(dir, "subservers.client"), false);
            config.put("Name", name);
            config.put("Address", host.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1").replace("0.0.0.0", "127.0.0.1"));
            config.put("Password", host.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password", ""));
            config.put("Encryption", host.plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE"));
            config.write(writer);
            writer.close();
        }
    }
    private void generateProperties(File dir, int port) throws IOException {
        File file = new File(dir, "server.properties");
        if (!file.exists()) file.createNewFile();
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
