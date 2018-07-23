package net.ME1312.SubServers.Client.Sponge;

import com.google.gson.Gson;
import com.google.inject.Inject;
import net.ME1312.SubServers.Client.Sponge.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Metrics;
import net.ME1312.SubServers.Client.Sponge.Library.NamedContainer;
import net.ME1312.SubServers.Client.Sponge.Library.UniversalFile;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Library.Version.VersionType;
import net.ME1312.SubServers.Client.Sponge.Network.Cipher;
import net.ME1312.SubServers.Client.Sponge.Network.SubDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SubServers Client Plugin Class
 */
@Plugin(id = "subservers-client-sponge", name = "SubServers-Client-Sponge", authors = "ME1312", version = "2.13a/pr5", url = "https://github.com/ME1312/SubServers-2", description = "Access your SubServers from Anywhere")
public final class SubPlugin {
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public YAMLConfig config;
    public SubDataClient subdata = null;

    @ConfigDir(sharedRoot = false)
    @Inject public File dir;
    public Logger logger = LoggerFactory.getLogger("SubServers");
    public UIHandler gui = null;
    public Version version;
    public SubAPI api;
    @Inject public PluginContainer plugin;
    @Inject public Game game;

    @Listener
    public void setup(GamePreInitializationEvent event) {
        if (plugin.getVersion().isPresent()) {
            //version = Version.fromString(plugin.getVersion().get());
            version = new Version(Version.fromString(plugin.getVersion().get()), VersionType.SNAPSHOT, (SubPlugin.class.getPackage().getSpecificationTitle() == null)?"undefined":SubPlugin.class.getPackage().getSpecificationTitle()); // TODO Snapshot Version
        } else version = new Version("undefined");
    }

    /**
     * Enable Plugin
     */
    @Listener
    @SuppressWarnings("unchecked")
    public void enable(GameInitializationEvent event) {
        api = new SubAPI(this);
        try {
            logger.info("Loading SubServers.Client.Sponge v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion() + ")");
            dir.mkdirs();
            if (new UniversalFile(dir.getParentFile(), "SubServers-Client:config.yml").exists()) {
                Files.move(new UniversalFile(dir.getParentFile(), "SubServers-Client:config.yml").toPath(), new UniversalFile(dir, "config.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                Util.deleteDirectory(new UniversalFile(dir.getParentFile(), "SubServers-Client"));
            }
            if (!(new UniversalFile(dir, "config.yml").exists())) {
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "config.yml", new UniversalFile(dir, "config.yml").getPath());
                logger.info("Created ~/plugins/SubServers/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(SubPlugin.class.getClassLoader(), "config.yml", new UniversalFile(dir, "config.yml").getPath());
                logger.info("Updated ~/plugins/SubServers/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client").exists()) {
                FileReader reader = new FileReader(new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client"));
                config.get().getSection("Settings").set("SubData", new YAMLSection(new Gson().fromJson(Util.readAll(reader), Map.class)));
                config.save();
                reader.close();
                new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client").delete();
            }

            reload(false);

            if (config.get().getSection("Settings").getBoolean("Ingame-Access", true)) {
                //gui = new InternalUIHandler(this);
                Sponge.getCommandManager().register(plugin, new SubCommand(this).spec(), "sub", "subserver", "subservers");
            }

            new Metrics(this);
            game.getScheduler().createTaskBuilder().async().execute(() -> {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Client.Sponge/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                    NodeList updnodeList = updxml.getElementsByTagName("version");
                    Version updversion = version;
                    int updcount = 0;
                    for (int i = 0; i < updnodeList.getLength(); i++) {
                        Node node = updnodeList.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if (!node.getTextContent().startsWith("-") && !node.getTextContent().equals(version.toString()) && Version.fromString(node.getTextContent()).compareTo(updversion) > 0) {
                                updversion = Version.fromString(node.getTextContent());
                                updcount++;
                            }
                        }
                    }
                    if (updcount > 0) logger.info("SubServers.Client.Sponge v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }).delay(0, TimeUnit.MILLISECONDS).interval(2, TimeUnit.DAYS).submit(plugin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload(boolean notifyPlugins) throws IOException {
        if (subdata != null)
            subdata.destroy(0);

        config.reload();

        Cipher cipher = null;
        if (!config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equalsIgnoreCase("NONE")) {
            if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                logger.info("Cannot encrypt connection without a password");
            } else if (!SubDataClient.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                logger.info("Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
            } else {
                cipher = SubDataClient.getCipher(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
            }
        }
        subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", null),
                InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), cipher);

        if (notifyPlugins) {
            List<Runnable> listeners = api.reloadListeners;
            if (listeners.size() > 0) {
                for (Object obj : listeners) {
                    try {
                        ((Runnable) obj).run();
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem reloading plugin").printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Disable Plugin
     */
    @Listener
    public void disable(GameStoppingEvent event) {
        if (subdata != null) try {
            subdata.destroy(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
