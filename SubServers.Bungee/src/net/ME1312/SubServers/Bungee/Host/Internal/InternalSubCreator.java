package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Event.SubCreateEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Container;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Exception.SubCreatorException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.UniversalFile;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.SubAPI;
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
import java.util.regex.Pattern;

/**
 * Internal SubCreator Class
 */
public class InternalSubCreator extends SubCreator {
    private HashMap<String, ServerTemplate> templates = new HashMap<String, ServerTemplate>();
    private InternalHost host;
    private String gitBash;
    private TreeMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>> thread;

    /**
     * Creates an Internal SubCreator
     *
     * @param host Host
     * @param gitBash Git Bash
     */
    public InternalSubCreator(InternalHost host, String gitBash) {
        if (Util.isNull(host, gitBash)) throw new NullPointerException();
        this.host = host;
        this.gitBash = (System.getenv("ProgramFiles(x86)") == null)?Pattern.compile("%(ProgramFiles)\\(x86\\)%", Pattern.CASE_INSENSITIVE).matcher(gitBash).replaceAll("%$1%"):gitBash;
        this.thread = new TreeMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>>();
        reload();
    }

    @Override
    public void reload() {
        templates.clear();
        if (new UniversalFile(host.plugin.dir, "SubServers:Templates").exists()) for (File file : new UniversalFile(host.plugin.dir, "SubServers:Templates").listFiles()) {
            try {
                if (file.isDirectory()) {
                    YAMLSection config = (new UniversalFile(file, "template.yml").exists())?new YAMLConfig(new UniversalFile(file, "template.yml")).get().getSection("Template", new YAMLSection()):new YAMLSection();
                    ServerTemplate template = new ServerTemplate(file.getName(), config.getBoolean("Enabled", true), config.getRawString("Icon", "::NULL::"), file, config.getSection("Build", new YAMLSection()), config.getSection("Settings", new YAMLSection()));
                    templates.put(file.getName().toLowerCase(), template);
                    if (config.getKeys().contains("Display")) template.setDisplayName(config.getString("Display"));
                }
            } catch (Exception e) {
                System.out.println(host.getName() + File.separator + "Creator > Couldn't load template: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private YAMLSection build(NamedContainer<InternalSubLogger, Process> thread, File dir, String name, ServerTemplate template, Version version, List<ServerTemplate> history) throws SubCreatorException {
        YAMLSection server = new YAMLSection();
        boolean error = false;
        if (history.contains(template)) throw new IllegalStateException("Template Import loop detected");
        history.add(template);
        for (String other : template.getBuildOptions().getStringList("Import", new ArrayList<String>())) {
            if (templates.keySet().contains(other.toLowerCase())) {
                YAMLSection config = build(thread, dir, other, templates.get(other.toLowerCase()), version, history);
                if (config == null) {
                    throw new SubCreatorException();
                } else {
                    server.setAll(config);
                }
            } else {
                System.out.println(name + File.separator + "Creator > Skipping missing template: " + other);
            }
        }
        server.setAll(template.getConfigOptions());
        try {
            System.out.println(name + File.separator + "Creator > Loading Template: " + template.getDisplayName());
            Util.copyDirectory(template.getDirectory(), dir);
            if (template.getType() == ServerType.VANILLA) {
                String patch = "Patch";
                if (version.compareTo(new Version("1.12")) >= 0) patch += "-v2";
                version = new Version(version.toString() + " " + patch);
            } else if (template.getType() == ServerType.SPONGE) {
                System.out.println(name + File.separator + "Creator > Searching Versions...");
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
                System.out.println(name + File.separator + "Creator > Found \"spongeforge-" + spversion.toString() + '"');

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
                System.out.println(name + File.separator + "Creator > Found \"forge-" + mcfversion.toString() + '"');

                version = new Version(mcfversion.toString() + " " + spversion.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (template.getBuildOptions().contains("Shell-Location")) {
            String gitBash = this.gitBash + ((this.gitBash.endsWith(File.separator))?"":File.separator) + "bin" + File.separatorChar + "bash.exe";
            if (!(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) && template.getBuildOptions().contains("Permission")) {
                try {
                    Process process = Runtime.getRuntime().exec("chmod " + template.getBuildOptions().getRawString("Permission") + ' ' + template.getBuildOptions().getRawString("Shell-Location"), null, dir);
                    Thread.sleep(500);
                    if (process.exitValue() != 0) {
                        System.out.println(name + File.separator + "Creator > Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    }
                } catch (Exception e) {
                    System.out.println(name + File.separator + "Creator > Couldn't set " + template.getBuildOptions().getRawString("Permission") + " permissions to " + template.getBuildOptions().getRawString("Shell-Location"));
                    e.printStackTrace();
                }
            }

            try {
                System.out.println(name + File.separator + "Creator > Launching " + template.getBuildOptions().getRawString("Shell-Location"));
                thread.set(Runtime.getRuntime().exec((System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) ? "cmd.exe /c \"\"" + gitBash + "\" --login -i -c \"bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + "\"\"" : ("bash " + template.getBuildOptions().getRawString("Shell-Location") + ' ' + version.toString() + " " + System.getProperty("user.home")), null, dir));
                thread.name().log.set(host.plugin.config.get().getSection("Settings").getBoolean("Log-Creator"));
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
                e.printStackTrace();
            }
        }

        new UniversalFile(dir, "template.yml").delete();
        if (error) throw new SubCreatorException();
        return server;
    }

    private void run(UUID player, String name, ServerTemplate template, Version version, int port) {
        NamedContainer<InternalSubLogger, Process> thread = this.thread.get(name.toLowerCase()).get();
        UniversalFile dir = new UniversalFile(new File(host.getPath()), name);
        dir.mkdirs();
        YAMLSection server = new YAMLSection();
        YAMLSection config;
        try {
            config = build(thread, dir, name, template, version, new LinkedList<>());
            generateProperties(dir, port);
            generateClient(dir, template.getType(), name);
        } catch (SubCreatorException e) {
            config = null;
        } catch (Exception e) {
            config = null;
            e.printStackTrace();
        }

        if (config != null) {
            try {
                System.out.println(name + File.separator + "Creator > Saving...");
                if (host.plugin.exServers.keySet().contains(name.toLowerCase())) host.plugin.exServers.remove(name.toLowerCase());

                for (String option : config.getKeys()) {
                    if (config.isString(option)) {
                        config.set(option, config.getRawString(option).replace("$name$", name).replace("$template$", template.getName()).replace("$type$", template.getType().toString())
                                .replace("$version$", version.toString().replace(" ", "@")).replace("$port$", Integer.toString(port)));
                    }
                }
                server.set("Enabled", true);
                //server.set("Editable", true);
                server.set("Display", "");
                server.set("Host", host.getName());
                server.set("Group", new ArrayList<String>());
                server.set("Port", port);
                server.set("Motd", "Some SubServer");
                server.set("Log", true);
                server.set("Directory", "." + File.separatorChar + name);
                server.set("Executable", "java -Xmx1024M -jar " + template.getType().toString() + ".jar");
                server.set("Stop-Command", "stop");
                server.set("Run-On-Launch", false);
                server.set("Auto-Restart", false);
                server.set("Restricted", false);
                server.set("Incompatible", new ArrayList<String>());
                server.set("Hidden", false);
                server.setAll(config);

                SubServer subserver = host.addSubServer(player, name, server.getBoolean("Enabled"), port, server.getColoredString("Motd", '&'), server.getBoolean("Log"), server.getRawString("Directory"),
                        new Executable(server.getRawString("Executable")), server.getRawString("Stop-Command"), true, server.getBoolean("Auto-Restart"), server.getBoolean("Hidden"), server.getBoolean("Restricted"), false);
                if (server.getString("Display").length() > 0) subserver.setDisplayName(server.getString("Display"));
                for (String group : server.getStringList("Group")) subserver.addGroup(group);
                if (server.contains("Extra")) for (String extra : server.getSection("Extra").getKeys()) subserver.addExtra(extra, server.getObject(extra));
                host.plugin.config.get().getSection("Servers").set(name, server);
                host.plugin.config.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(name + File.separator + "Creator > Couldn't build the server jar. Check the SubCreator logs for more detail.");
        }
        this.thread.remove(name.toLowerCase());
    }

    @Override
    public boolean create(UUID player, String name, ServerTemplate template, Version version, int port) {
        if (Util.isNull(name, template, version, port)) throw new NullPointerException();
        if (template.isEnabled() && !SubAPI.getInstance().getSubServers().keySet().contains(name.toLowerCase()) && !SubCreator.isReserved(name)) {
            NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>> thread = new NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>(null, new NamedContainer<InternalSubLogger, Process>(new InternalSubLogger(null, this, name + File.separator + "Creator", new Container<Boolean>(false), null), null));
            this.thread.put(name.toLowerCase(), thread);

            final SubCreateEvent event = new SubCreateEvent(player, host, name, template, version, port);
            host.plugin.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                thread.rename(new Thread(() -> InternalSubCreator.this.run(player, name, event.getTemplate(), event.getVersion(), port)));
                thread.name().start();
                return true;
            } else {
                this.thread.remove(name.toLowerCase());
                return false;
            }
        } else return false;
    }

    @Override
    public void terminate() {
        HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            terminate(i);
        }
    }

    @Override
    public void terminate(String name) {
        if (this.thread.get(name.toLowerCase()).get().get() != null && this.thread.get(name.toLowerCase()).get().get().isAlive()) {
            this.thread.get(name.toLowerCase()).get().get().destroyForcibly();
        } else if (this.thread.get(name.toLowerCase()).name() != null && this.thread.get(name.toLowerCase()).name().isAlive()) {
            this.thread.get(name.toLowerCase()).name().interrupt();
            this.thread.remove(name.toLowerCase());
        }
    }

    @Override
    public void waitFor() throws InterruptedException {
        HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            waitFor(i);
        }
    }

    @Override
    public void waitFor(String name) throws InterruptedException {
        while (this.thread.keySet().contains(name.toLowerCase()) && this.thread.get(name.toLowerCase()).name() != null && this.thread.get(name.toLowerCase()).name().isAlive()) {
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
    public List<SubLogger> getLogger() {
        List<SubLogger> loggers = new ArrayList<SubLogger>();
        HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>> temp = new HashMap<String, NamedContainer<Thread, NamedContainer<InternalSubLogger, Process>>>();
        temp.putAll(thread);
        for (String i : temp.keySet()) {
            loggers.add(getLogger(i));
        }
        return loggers;
    }

    @Override
    public SubLogger getLogger(String name) {
        return this.thread.get(name.toLowerCase()).get().name();
    }

    @Override
    public List<String> getReservedNames() {
        return new ArrayList<String>(thread.keySet());
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
                // if (!new UniversalFile(dir, "mods").exists()) new UniversalFile(dir, "mods").mkdirs();
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
        String content = Util.readAll(new BufferedReader(new InputStreamReader(new FileInputStream(file)))).replace("server-port=", "server-port=" + port).replace("server-ip=", "server-ip=" + host.getAddress().getHostAddress());
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