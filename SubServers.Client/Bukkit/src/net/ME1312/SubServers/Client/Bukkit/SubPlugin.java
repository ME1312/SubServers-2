package net.ME1312.SubServers.Client.Bukkit;

import com.google.gson.Gson;
import net.ME1312.SubServers.Client.Bukkit.Graphic.InternalUIHandler;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Metrics;
import net.ME1312.SubServers.Client.Bukkit.Library.NamedContainer;
import net.ME1312.SubServers.Client.Bukkit.Library.UniversalFile;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.VersionType;
import net.ME1312.SubServers.Client.Bukkit.Network.Cipher;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
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
public final class SubPlugin extends JavaPlugin {
    protected NamedContainer<Long, Map<String, Map<String, String>>> lang = null;
    public YAMLConfig config;
    public SubDataClient subdata = null;

    public UIHandler gui = null;
    public final Version version;
    public final SubAPI api = new SubAPI(this);

    public SubPlugin() {
        super();
        version = Version.fromString(getDescription().getVersion());
        //version = new Version(Version.fromString(getDescription().getVersion()), VersionType.SNAPSHOT, (SubPlugin.class.getPackage().getSpecificationTitle() == null)?"?":SubPlugin.class.getPackage().getSpecificationTitle()); // TODO Snapshot Version
    }

    /**
     * Enable Plugin
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEnable() {
        try {
            Bukkit.getLogger().info("SubServers > Loading SubServers.Client.Bukkit v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion() + ")");
            getDataFolder().mkdirs();
            if (new UniversalFile(getDataFolder().getParentFile(), "SubServers-Client:config.yml").exists()) {
                Files.move(new UniversalFile(getDataFolder().getParentFile(), "SubServers-Client:config.yml").toPath(), new UniversalFile(getDataFolder(), "config.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
                Util.deleteDirectory(new UniversalFile(getDataFolder().getParentFile(), "SubServers-Client"));
            }
            if (!(new UniversalFile(getDataFolder(), "config.yml").exists())) {
                Util.copyFromJar(SubPlugin.class.getClassLoader(), "config.yml", new UniversalFile(getDataFolder(), "config.yml").getPath());
                Bukkit.getLogger().info("SubServers > Created ~/plugins/SubServers/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
                Files.move(new UniversalFile(getDataFolder(), "config.yml").toPath(), new UniversalFile(getDataFolder(), "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(SubPlugin.class.getClassLoader(), "config.yml", new UniversalFile(getDataFolder(), "config.yml").getPath());
                Bukkit.getLogger().info("SubServers > Updated ~/plugins/SubServers/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"));
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client").exists()) {
                FileReader reader = new FileReader(new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client"));
                config.get().getSection("Settings").set("SubData", new YAMLSection(new Gson().fromJson(Util.readAll(reader), Map.class)));
                config.save();
                reader.close();
                new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client").delete();
            }

            reload(false);

            if (config.get().getSection("Settings").getBoolean("Ingame-Access", true)) {
                gui = new InternalUIHandler(this);
                SubCommand cmd = new SubCommand(this);
                getCommand("subservers").setExecutor(cmd);
                getCommand("subserver").setExecutor(cmd);
                getCommand("sub").setExecutor(cmd);
            }

            new Metrics(this);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Client.Bukkit/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

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
                    if (updcount > 0) System.out.println("SubServers > SubServers.Client.Bukkit v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }, 0, TimeUnit.DAYS.toSeconds(2) * 20);
        } catch (IOException e) {
            setEnabled(false);
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
                System.out.println("SubData > Cannot encrypt connection without a password");
            } else if (!SubDataClient.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                System.out.println("SubData > Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
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
    @Override
    public void onDisable() {
        if (subdata != null) try {
            subdata.destroy(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setEnabled(false);
    }
}
