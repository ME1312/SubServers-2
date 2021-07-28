package net.ME1312.SubServers.Client.Bukkit;

import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Encryption.AES;
import net.ME1312.SubData.Client.Encryption.DHE;
import net.ME1312.SubData.Client.Encryption.RSA;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubServers.Client.Bukkit.Graphic.DefaultUIHandler;
import net.ME1312.SubServers.Client.Bukkit.Graphic.UIHandler;
import net.ME1312.SubServers.Client.Bukkit.Library.ConfigUpdater;
import net.ME1312.SubServers.Client.Bukkit.Library.Metrics;
import net.ME1312.SubServers.Client.Bukkit.Network.SubProtocol;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.ME1312.SubServers.Client.Bukkit.Library.AccessMode.*;

/**
 * SubServers Client Plugin Class
 */
public final class SubPlugin extends JavaPlugin {
    HashMap<Integer, SubDataClient> subdata = new HashMap<Integer, SubDataClient>();
    Pair<Long, Map<String, Map<String, String>>> lang = null;
    private UniversalFile dir;
    public YAMLConfig config;
    public SubProtocol subprotocol;

    public UIHandler gui = null;
    public final Version version;
    public final SubAPI api = new SubAPI(this);
    public String server_address;

    private long resetDate = 0;
    private boolean reconnect = false;

    public SubPlugin() {
        super();
        version = Version.fromString(getDescription().getVersion());
        subdata.put(0, null);
    }

    /**
     * Enable Plugin
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEnable() {
        try {
            Bukkit.getLogger().info("SubServers > Loading SubServers.Client.Bukkit v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion() + ")");
            dir = new UniversalFile(getDataFolder().getParentFile(), "SubServers-Client");
            if (getDataFolder().exists()) {
                Files.move(getDataFolder().toPath(), dir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else dir.mkdirs();
            ConfigUpdater.updateConfig(new UniversalFile(dir, "config.yml"));
            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json").exists()) {
                FileReader reader = new FileReader(new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json"));
                config.get().getMap("Settings").set("SubData", new YAMLSection(parseJSON(Util.readAll(reader))));
                config.save();
                reader.close();
                new UniversalFile(new File(System.getProperty("user.dir")), "subdata.json").delete();
            }
            if (new UniversalFile(new File(System.getProperty("user.dir")), "subdata.rsa.key").exists()) {
                if (new UniversalFile(dir, "subdata.rsa.key").exists()) new UniversalFile(dir, "subdata.rsa.key").delete();
                Files.move(new UniversalFile(new File(System.getProperty("user.dir")), "subdata.rsa.key").toPath(), new UniversalFile(dir, "subdata.rsa.key").toPath());
            }

            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            reload(false);

            subprotocol = SubProtocol.get();
            subprotocol.registerCipher("DHE", DHE.get(128));
            subprotocol.registerCipher("DHE-128", DHE.get(128));
            subprotocol.registerCipher("DHE-192", DHE.get(192));
            subprotocol.registerCipher("DHE-256", DHE.get(256));
            api.name = config.get().getMap("Settings").getMap("SubData").getString("Name", System.getenv("name"));
            server_address = config.get().getMap("Settings").getRawString("Connect-Address", System.getenv("address"));

            if (config.get().getMap("Settings").getMap("SubData").getRawString("Password", "").length() > 0) {
                subprotocol.registerCipher("AES", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-128", new AES(128, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-192", new AES(192, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));
                subprotocol.registerCipher("AES-256", new AES(256, config.get().getMap("Settings").getMap("SubData").getRawString("Password")));

                System.out.println("SubData > AES Encryption Available");
            }
            if (new UniversalFile(dir, "subdata.rsa.key").exists()) {
                try {
                    subprotocol.registerCipher("RSA", new RSA(new UniversalFile(dir, "subdata.rsa.key")));
                    System.out.println("SubData > RSA Encryption Available");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            reconnect = true;
            System.out.println("SubData > ");
            System.out.println("SubData > Connecting to /" + config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391"));
            connect(null);

            gui = new DefaultUIHandler(this);
            if (api.access.value > NO_COMMANDS.value && !config.get().getMap("Settings").getBoolean("API-Only-Mode", false)) {
                CommandMap cmd = Util.reflect(Bukkit.getServer().getClass().getDeclaredField("commandMap"), Bukkit.getServer());

                cmd.register("subservers", new SubCommand(this, "subservers"));
                cmd.register("subservers", new SubCommand(this, "subserver"));
                cmd.register("subservers", new SubCommand(this, "sub"));
            }

            if (api.access.value > NO_INTEGRATIONS.value && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new net.ME1312.SubServers.Client.Bukkit.Library.Compatibility.PlaceholderImpl(this).register();
            }

            new Metrics(this, 2334);
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
        resetDate = Calendar.getInstance().getTime().getTime();

        ConfigUpdater.updateConfig(new UniversalFile(dir, "config.yml"));
        config.reload();

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

    private void connect(Pair<DisconnectReason, DataClient> disconnect) throws IOException {
        int reconnect = config.get().getMap("Settings").getMap("SubData").getInt("Reconnect", 60);
        if (disconnect == null || (this.reconnect && reconnect > 0 && disconnect.key() != DisconnectReason.PROTOCOL_MISMATCH && disconnect.key() != DisconnectReason.ENCRYPTION_MISMATCH)) {
            long reset = resetDate;
            if (disconnect != null) Bukkit.getLogger().info("SubData > Attempting reconnect in " + reconnect + " seconds");
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (reset == resetDate && (subdata.getOrDefault(0, null) == null || subdata.get(0).isClosed())) {
                            SubDataClient open = subprotocol.open(InetAddress.getByName(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                                    Integer.parseInt(config.get().getMap("Settings").getMap("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]));

                            if (subdata.getOrDefault(0, null) != null) subdata.get(0).reconnect(open);
                            subdata.put(0, open);
                        }
                    } catch (IOException e) {
                        Bukkit.getLogger().info("SubData > Connection was unsuccessful, retrying in " + reconnect + " seconds");

                        Bukkit.getScheduler().runTaskLater(SubPlugin.this, this, reconnect * 20);
                    }
                }
            }, (disconnect == null)?0:reconnect * 20);
        }
    }

    /**
     * Disable Plugin
     */
    @Override
    public void onDisable() {
        if (subdata != null) try {
            setEnabled(false);
            reconnect = false;

            ArrayList<SubDataClient> temp = new ArrayList<SubDataClient>();
            temp.addAll(subdata.values());
            for (SubDataClient client : temp) if (client != null)  {
                client.close();
                client.waitFor();
            }
            subdata.clear();
            subdata.put(0, null);
        } catch (InterruptedException e) {
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

    /**
     * Send a message to the BungeeCord Plugin Messaging Channel
     *
     * @param player Player that will send
     * @param message Message contents
     */
    public void pmc(Player player, String... message) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(stream);

        try {
            for (String m : message) data.writeUTF(m);
            data.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendPluginMessage(this, "BungeeCord", stream.toByteArray());
    }
}
