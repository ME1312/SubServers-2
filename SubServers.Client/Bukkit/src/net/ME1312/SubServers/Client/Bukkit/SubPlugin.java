package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.SubServers.Client.Bukkit.Graphic.InternalUIHandler;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Client.Bukkit.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.UniversalFile;
import net.ME1312.SubServers.Client.Bukkit.Library.Util;
import net.ME1312.SubServers.Client.Bukkit.Library.Version.Version;
import net.ME1312.SubServers.Client.Bukkit.Network.SubDataClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * SubServers Client Plugin Class
 */
public final class SubPlugin extends JavaPlugin {
    public YAMLConfig config;
    public YAMLSection lang = null;
    public SubDataClient subdata = null;

    public UIHandler gui = null;
    public final Version version;
    public final Version bversion = new Version(2);
    public final SubAPI api = new SubAPI(this);

    public SubPlugin() {
        super();
        version = new Version(getDescription().getVersion());
    }

    /**
     * Enable Plugin
     */
    @Override
    public void onEnable() {
        try {
            Bukkit.getLogger().info("SubServers > Loading SubServers.Client.Bukkit v" + version.toString() + " Libraries... ");
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
                config.get().getSection("Settings").set("SubData", new JSONObject(Util.readAll(new FileReader(new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client")))));
                config.save();
                new UniversalFile(new File(System.getProperty("user.dir")), "subservers.client").delete();
            }
            SubDataClient.Encryption encryption = SubDataClient.Encryption.NONE;
            if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                System.out.println("SubData > Cannot encrypt connection without a password");
            } else if (Util.isException(() -> SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase()))) {
                System.out.println("SubData > Unknown encryption type: " + SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "None")));
            } else {
                encryption = SubDataClient.Encryption.valueOf(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").replace('-', '_').replace(' ', '_').toUpperCase());
            }
            subdata = new SubDataClient(this, config.get().getSection("Settings").getSection("SubData").getString("Name", "undefined"),
                    InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[0]),
                    Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getString("Address", "127.0.0.1:4391").split(":")[1]), encryption);

            if (config.get().getSection("Settings").getBoolean("Ingame-Access", true)) {
                gui = new InternalUIHandler(this);
                SubCommand cmd = new SubCommand(this);
                getCommand("subservers").setExecutor(cmd);
                getCommand("subserver").setExecutor(cmd);
                getCommand("sub").setExecutor(cmd);
            }

            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    Document updxml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(Util.readAll(new BufferedReader(new InputStreamReader(new URL("http://src.me1312.net/maven/net/ME1312/SubServers/SubServers.Client.Bukkit/maven-metadata.xml").openStream(), Charset.forName("UTF-8")))))));

                    NodeList updnodeList = updxml.getElementsByTagName("version");
                    Version updversion = version;
                    int updcount = -1;
                    for (int i = 0; i < updnodeList.getLength(); i++) {
                        Node node = updnodeList.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if (!node.getTextContent().startsWith("-") && new Version(node.getTextContent()).compareTo(updversion) >= 0) {
                                updversion = new Version(node.getTextContent());
                                updcount++;
                            }
                        }
                    }
                    if (!updversion.equals(version)) System.out.println("SubServers > SubServers.Client.Bukkit v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }, 0, TimeUnit.DAYS.toSeconds(2) * 20);
        } catch (IOException e) {
            setEnabled(false);
            e.printStackTrace();
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
