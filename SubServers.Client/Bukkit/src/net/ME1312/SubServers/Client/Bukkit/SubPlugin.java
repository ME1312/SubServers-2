package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubServers.Client.Bukkit.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Client.Bukkit.Graphic.DefaultUIHandler;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Bukkit.Library.Metrics;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Network.Packet.PacketLinkServer;
import net.ME1312.SubServers.Client.Bukkit.Network.SubProtocol;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedList;
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
    public SubDataProtocol subprotocol;

    public UIHandler gui = null;
    public final Version version;
    public final SubAPI api = new SubAPI(this);

    public SubPlugin() {
        super();
        version = Version.fromString(getDescription().getVersion());
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
                Bukkit.getLogger().info("SubServers > Created ~/plugins/SubServers-Client-Bukkit/config.yml");
            } else if (((new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"))).get().getMap("Settings").getVersion("Version", new Version(0))).compareTo(new Version("2.11.2a+")) != 0) {
                Files.move(new UniversalFile(getDataFolder(), "config.yml").toPath(), new UniversalFile(getDataFolder(), "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(SubPlugin.class.getClassLoader(), "config.yml", new UniversalFile(getDataFolder(), "config.yml").getPath());
                Bukkit.getLogger().info("SubServers > Updated ~/plugins/SubServers-Client-Bukkit/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(getDataFolder(), "config.yml"));
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json").exists()) {
                FileReader reader = new FileReader(new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json"));
                config.get().getMap("Settings").set("SubData", new YAMLSection(parseJSON(Util.readAll(reader))));
                config.save();
                reader.close();
                new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json").delete();
            }
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subdata.rsa.key").exists()) {
                Files.move(new UniversalFile(new File(System.getProperty("user.dir")), "subdata.rsa.key").toPath(), new UniversalFile(getDataFolder(), "subdata.rsa.key").toPath());
            }

            subprotocol = SubProtocol.get();
            reload(false);

            if (config.get().getMap("Settings").getBoolean("Ingame-Access", true)) {
                gui = new DefaultUIHandler(this);
                SubCommand cmd = new SubCommand(this);
                getCommand("subservers").setExecutor(cmd);
                getCommand("subserver").setExecutor(cmd);
                getCommand("sub").setExecutor(cmd);
            }

            new Metrics(this);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    YAMLSection tags = new YAMLSection(parseJSON("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}'));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = version;
                    int updcount = 0;
                    for (ObjectMap<String> tag : tags.getMapList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                    Collections.sort(versions);
                    for (Version version : versions) {
                        if (version.compareTo(updversion) > 0) {
                            updversion = version;
                            updcount++;
                        }
                    }
                    if (updcount > 0) Bukkit.getLogger().info("SubServers > SubServers.Client.Bukkit v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }, 0, TimeUnit.DAYS.toSeconds(2) * 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload(boolean notifyPlugins) throws IOException {
        if (subdata != null)
            subdata.close();

        config.reload();

        subprotocol.unregisterCipher("AES");
        subprotocol.unregisterCipher("AES-128");
        subprotocol.unregisterCipher("AES-192");
        subprotocol.unregisterCipher("AES-256");
        subprotocol.unregisterCipher("RSA");
        api.name = config.get().getMap("Settings").getMap("SubData").getString("Name", null);

        if (config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) {
            subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
            subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));

            System.out.println("SubData > AES Encryption Available");
        }
        if (new UniversalFile(getDataFolder(), "subdata.rsa.key").exists()) {
            try {
                subprotocol.registerCipher("RSA", new RSA(new UniversalFile(getDataFolder(), "subdata.rsa.key")));
                System.out.println("SubData > RSA Encryption Available");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("SubData > ");
        subdata = subprotocol.open((config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]));

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
            subdata.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Use reflection to access Gson for parsing
     *
     * @param json JSON to parse
     * @return JSON as a map
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public Map<String, ?> parseJSON(String json) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        Class<?> gson = Class.forName(((Util.getDespiteException(() -> Class.forName("com.google.gson.Gson") != null, false)?"":"org.bukkit.craftbukkit.libs.")) + "com.google.gson.Gson");
        //Class<?> gson = com.google.gson.Gson.class;
        return (Map<String, ?>) gson.getMethod("fromJson", String.class, Class.class).invoke(gson.newInstance(), json, Map.class);
    }
}
