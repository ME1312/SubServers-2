package net.ME1312.SubServers.Sync.Network;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.ME1312.SubServers.Sync.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Sync.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import net.ME1312.SubServers.Sync.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Sync.Library.NamedContainer;
import net.ME1312.SubServers.Sync.Library.Util;
import net.ME1312.SubServers.Sync.Library.Version.Version;
import net.ME1312.SubServers.Sync.Network.Encryption.AES;
import net.ME1312.SubServers.Sync.Network.Packet.*;
import net.ME1312.SubServers.Sync.SubPlugin;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.conf.Configuration;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * SubData Direct Client Class
 */
public final class SubDataClient {
    private static HashMap<Class<? extends PacketOut>, NamedContainer<String, String>> pOut = new HashMap<Class<? extends PacketOut>, NamedContainer<String, String>>();
    private static HashMap<String, HashMap<String, List<PacketIn>>> pIn = new HashMap<String, HashMap<String, List<PacketIn>>>();
    private static HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    private static boolean defaults = false;
    private PrintWriter writer;
    private NamedContainer<Boolean, Socket> socket;
    private String name = null;
    private Cipher cipher;
    private SubPlugin plugin;
    private LinkedList<NamedContainer<String, PacketOut>> queue;

    /**
     * SubServers Client Instance
     *
     * @param plugin SubPlugin
     * @param address Address
     * @param port Port
     * @param cipher Cipher
     * @throws IOException
     */
    public SubDataClient(SubPlugin plugin, String name, InetAddress address, int port, Cipher cipher) throws IOException {
        if (Util.isNull(plugin, address, port)) throw new NullPointerException();
        socket = new NamedContainer<>(false, new Socket(address, port));
        this.plugin = plugin;
        this.name = name;
        this.writer = new PrintWriter(socket.get().getOutputStream(), true);
        this.cipher = (cipher != null)?cipher:new Cipher() {
            @Override
            public String getName() {
                return "NONE";
            }
            @Override
            public byte[] encrypt(String key, YAMLSection data) {
                return data.toJSON().getBytes(StandardCharsets.UTF_8);
            }
            @Override
            @SuppressWarnings("unchecked")
            public YAMLSection decrypt(String key, byte[] data) {
                return new YAMLSection(new Gson().fromJson(new String(data, StandardCharsets.UTF_8), Map.class));
            }
        };
        this.queue = new LinkedList<NamedContainer<String, PacketOut>>();

        if (!defaults) loadDefaults();
        loop();

        sendPacket(new NamedContainer<>(null, new PacketAuthorization(plugin)));
    }

    private void init() {
        plugin.subdata.sendPacket(new PacketDownloadLang(plugin));
        plugin.subdata.sendPacket(new PacketLinkProxy(plugin));
        plugin.subdata.sendPacket(new PacketDownloadProxyInfo(proxy -> plugin.subdata.sendPacket(new PacketDownloadServerList(null, null, data -> {
            if (plugin.lastReload != proxy.getSection("subservers").getLong("last-reload")) {
                System.out.println("SubServers > Resetting Server Data");
                plugin.servers.clear();
                plugin.lastReload = proxy.getSection("subservers").getLong("last-reload");
            }
            try {
                LinkedList<ListenerInfo> listeners = new LinkedList<ListenerInfo>(plugin.getConfig().getListeners());
                for (int i = 0; i < proxy.getSection("bungee").getSectionList("listeners").size(); i++) if (i < listeners.size()) {
                    if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Forced-Hosts", true)) updateField(ListenerInfo.class.getDeclaredField("forcedHosts"), listeners.get(i), proxy.getSection("bungee").getSectionList("listeners").get(i).getSection("forced-hosts").get());
                    if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Motd", false)) updateField(ListenerInfo.class.getDeclaredField("motd"), listeners.get(i), proxy.getSection("bungee").getSectionList("listeners").get(i).getRawString("motd"));
                    if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Player-Limit", false)) updateField(ListenerInfo.class.getDeclaredField("maxPlayers"), listeners.get(i), proxy.getSection("bungee").getSectionList("listeners").get(i).getInt("player-limit"));
                    if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Server-Priorities", true)) updateField(ListenerInfo.class.getDeclaredField("serverPriority"), listeners.get(i), proxy.getSection("bungee").getSectionList("listeners").get(i).getRawStringList("priorities"));
                }
                if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Disabled-Commands", false)) updateField(Configuration.class.getDeclaredField("disabledCommands"), plugin.getConfig(), proxy.getSection("bungee").getRawStringList("disabled-cmds"));
                if (plugin.config.get().getSection("Sync", new YAMLSection()).getBoolean("Player-Limit", false)) updateField(Configuration.class.getDeclaredField("playerLimit"), plugin.getConfig(), proxy.getSection("bungee").getInt("player-limit"));
            } catch (Exception e) {
                System.out.println("SubServers > Problem syncing BungeeCord configuration options");
                e.printStackTrace();
            }
            for (String host : data.getSection("hosts").getKeys()) {
                for (String subserver : data.getSection("hosts").getSection(host).getSection("servers").getKeys()) {
                    plugin.merge(subserver, data.getSection("hosts").getSection(host).getSection("servers").getSection(subserver), true);
                }
            }
            for (String server : data.getSection("servers").getKeys()) {
                plugin.merge(server, data.getSection("servers").getSection(server), false);
            }
        }))));
        while (queue.size() != 0) {
            sendPacket(queue.get(0));
            queue.remove(0);
        }
        socket.rename(true);
        plugin.getPluginManager().callEvent(new SubNetworkConnectEvent(this));
    }
    private void updateField(Field field, Object instance, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(instance, value);
    }

    static {
        addCipher("AES", new AES(128));
        addCipher("AES_128", new AES(128));
        addCipher("AES_192", new AES(192));
        addCipher("AES_256", new AES(256));
    } private void loadDefaults() {
        defaults = true;

        registerPacket(new PacketAuthorization(plugin), "SubData", "Authorization");
        registerPacket(new PacketCommandServer(), "SubServers", "CommandServer");
        registerPacket(new PacketCreateServer(), "SubServers", "CreateServer");
        registerPacket(new PacketDownloadHostInfo(), "SubServers", "DownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubServers", "DownloadLang");
        registerPacket(new PacketDownloadNetworkList(), "SubServers", "DownloadNetworkList");
        registerPacket(new PacketDownloadPlayerList(), "SubServers", "DownloadPlayerList");
        registerPacket(new PacketDownloadProxyInfo(), "SubServers", "DownloadProxyInfo");
        registerPacket(new PacketDownloadServerInfo(), "SubServers", "DownloadServerInfo");
        registerPacket(new PacketDownloadServerList(), "SubServers", "DownloadServerList");
        registerPacket(new PacketInRunEvent(), "SubServers", "RunEvent");
        registerPacket(new PacketInReset(), "SubServers", "Reset");
        registerPacket(new PacketLinkProxy(plugin), "SubServers", "LinkProxy");
        registerPacket(new PacketStartServer(), "SubServers", "StartServer");
        registerPacket(new PacketStopServer(), "SubServers", "StopServer");

        registerPacket(PacketAuthorization.class, "SubData", "Authorization");
        registerPacket(PacketCommandServer.class, "SubServers", "CommandServer");
        registerPacket(PacketCreateServer.class, "SubServers", "CreateServer");
        registerPacket(PacketDownloadHostInfo.class, "SubServers", "DownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubServers", "DownloadLang");
        registerPacket(PacketDownloadNetworkList.class, "SubServers", "DownloadNetworkList");
        registerPacket(PacketDownloadPlayerList.class, "SubServers", "DownloadPlayerList");
        registerPacket(PacketDownloadProxyInfo.class, "SubServers", "DownloadProxyInfo");
        registerPacket(PacketDownloadServerInfo.class, "SubServers", "DownloadServerInfo");
        registerPacket(PacketDownloadServerList.class, "SubServers", "DownloadServerList");
        registerPacket(PacketLinkProxy.class, "SubServers", "LinkProxy");
        registerPacket(PacketStartServer.class, "SubServers", "StartServer");
        registerPacket(PacketStopServer.class, "SubServers", "StopServer");
    }

    private void loop() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.get().getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    try {
                        YAMLSection data = cipher.decrypt(plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), Base64.getDecoder().decode(input));
                        for (PacketIn packet : decodePacket(data)) {
                            try {
                                packet.execute((data.contains("c")) ? data.getSection("c") : null);
                            } catch (Throwable e) {
                                new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                            }
                        }
                    } catch (JsonParseException | YAMLException e) {
                        new IllegalPacketException("Unknown Packet Format: " + input).printStackTrace();
                    } catch (IllegalPacketException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        new InvocationTargetException(e, "Exception while decoding packet").printStackTrace();
                    }
                }
                try {
                    destroy(plugin.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
                try {
                    destroy(plugin.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Gets the Assigned Proxy Name
     *
     * @return Host Name
     */
    @SuppressWarnings("unchecked")
    public String getName() {
        if (name != null) {
            return name;
        } else if (plugin.redis) {
            try {
                return (String) plugin.redis("getServerId");
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public Socket getClient() {
        return socket.get();
    }

    /**
     * Add a Cipher for use by SubData
     *
     * @param cipher Cipher to Add
     * @param handle Handle to Bind
     */
    public static void addCipher(String handle, Cipher cipher) {
        if (Util.isNull(cipher)) throw new NullPointerException();
        if (ciphers.keySet().contains(handle.toUpperCase().replace('-', '_').replace(' ', '_'))) throw new IllegalStateException("Cipher already exists: " + handle);
        ciphers.put(handle.toUpperCase().replace('-', '_').replace(' ', '_'), cipher);
    }

    /**
     * Gets the Ciphers
     *
     * @return Cipher Map
     */
    public static Map<String, Cipher> getCiphers() {
        return new TreeMap<>(ciphers);
    }

    /**
     * Gets the Client's Cipher
     *
     * @return Cipher
     */
    public Cipher getCipher() {
        return cipher;
    }

    /**
     * Gets a Cipher by Handle
     *
     * @param handle Handle
     * @return Cipher
     */
    public static Cipher getCipher(String handle) {
        return getCiphers().get(handle.toUpperCase().replace('-', '_').replace(' ', '_'));
    }

    /**
     * Register PacketIn to the Network
     *
     * @param packet PacketIn to register
     * @param channel Packet Channel
     * @param handle Handle to Bind
     */
    public static void registerPacket(PacketIn packet, String channel, String handle) {
        if (Util.isNull(packet, channel, handle)) throw new NullPointerException();
        HashMap<String, List<PacketIn>> map = (pIn.keySet().contains(channel.toLowerCase()))?pIn.get(channel.toLowerCase()):new HashMap<String, List<PacketIn>>();
        List<PacketIn> list = (map.keySet().contains(handle))?map.get(handle):new ArrayList<PacketIn>();
        if (!list.contains(packet)) {
            list.add(packet);
            map.put(handle, list);
            pIn.put(channel.toLowerCase(), map);
        }
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param channel Packet Channel
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(String channel, PacketIn packet) {
        if (Util.isNull(channel, packet)) throw new NullPointerException();
        if (pIn.keySet().contains(channel.toLowerCase())) {
            List<String> search = new ArrayList<String>();
            search.addAll(pIn.get(channel.toLowerCase()).keySet());
            for (String handle : search) if (pIn.get(channel.toLowerCase()).get(handle).contains(packet)) {
                List<PacketIn> list = pIn.get(channel.toLowerCase()).get(handle);
                list.remove(packet);
                if (list.isEmpty()) {
                    pIn.get(channel.toLowerCase()).remove(handle);
                    if (pIn.get(channel.toLowerCase()).isEmpty()) pIn.remove(channel.toLowerCase());
                } else {
                    pIn.get(channel.toLowerCase()).put(handle, list);
                }
            }
        }
    }

    /**
     * Register PacketOut to the Network
     *
     * @param packet PacketOut to register
     * @param channel Packet Channel
     * @param handle Handle to bind
     */
    public static void registerPacket(Class<? extends PacketOut> packet, String channel, String handle) {
        if (Util.isNull(packet, channel, handle)) throw new NullPointerException();
        pOut.put(packet, new NamedContainer<String, String>(channel.toLowerCase(), handle));
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param channel Packet Channel
     * @param packet PacketOut to unregister
     */
    public static void unregisterPacket(String channel, Class<? extends PacketOut> packet) {
        if (Util.isNull(channel, packet)) throw new NullPointerException();
        if (pOut.keySet().contains(packet) && pOut.get(packet).name().equalsIgnoreCase(channel)) pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instances via handle
     *
     * @param channel Packet Channel
     * @param handle Handle
     * @return PacketIn
     */
    public static List<? extends PacketIn> getPacket(String channel, String handle) {
        if (Util.isNull(channel, handle)) throw new NullPointerException();
        return new ArrayList<PacketIn>(pIn.get(channel.toLowerCase()).get(handle));
    }

    /**
     * Send Packet to Server
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (socket.get() == null || !socket.name()) {
            queue.add(new NamedContainer<>(null, packet));
        } else {
            sendPacket(new NamedContainer<>(null, packet));
        }
    }

    private void sendPacket(NamedContainer<String, PacketOut> packet) {
        try {
            YAMLSection data = encodePacket(packet.get());
            if (packet.name() != null) data.set("f", packet.name());
            writer.println(Base64.getEncoder().encodeToString(cipher.encrypt(plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), data)));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Forward Packet to Client
     *
     * @param packet Packet to send
     * @param location Where to send
     */
    public void forwardPacket(PacketOut packet, String location) {
        if (Util.isNull(packet, location)) throw new NullPointerException();
        if (socket.get() == null || !socket.name()) {
            queue.add(new NamedContainer<>(location, packet));
        } else {
            sendPacket(new NamedContainer<>(null, packet));
        }
    }

    /**
     * Broadcast packet to all Clients
     *
     * @param packet Packet to send
     */
    public void broadcastPacket(PacketOut packet) {
        forwardPacket(packet, "");
    }

    /**
     * Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    private static YAMLSection encodePacket(PacketOut packet) throws IllegalPacketException, InvocationTargetException {
        YAMLSection json = new YAMLSection();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException("Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException("PacketOut Version cannot be null: " + packet.getClass().getCanonicalName());

        try {
            YAMLSection contents = packet.generate();
            json.set("n", pOut.get(packet.getClass()).name());
            json.set("h", pOut.get(packet.getClass()).get());
            json.set("v", packet.getVersion().toString());
            if (contents != null) json.set("c", contents);
            return json;
        } catch (Throwable e) {
            throw new InvocationTargetException(e, "Exception while encoding packet");
        }
    }

    /**
     * Decode PacketIn
     *
     * @param data Data to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     * @throws InvocationTargetException
     */
    private static List<PacketIn> decodePacket(YAMLSection data) throws IllegalPacketException, InvocationTargetException {
        if (!data.contains("h") || !data.contains("v")) throw new IllegalPacketException("Unknown Packet Format: " + data.toString());
        if (!pIn.keySet().contains(data.getRawString("n")) || !pIn.get(data.getRawString("n")).keySet().contains(data.getRawString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + data.getRawString("n") + ':' + data.getRawString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(data.getRawString("n")).get(data.getRawString("h"))) {
            if (packet.isCompatible(new Version(data.getRawString("v")))) {
                list.add(packet);
            } else {
                new IllegalPacketException("Packet Version Mismatch in " + data.getRawString("h") + ": " + data.getRawString("v") + " -> " + packet.getVersion().toString()).printStackTrace();
            }
        }

        return list;
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy(int reconnect) throws IOException {
        if (Util.isNull(reconnect)) throw new NullPointerException();
        if (socket.get() != null) {
            final Socket socket = this.socket.get();
            this.socket.set(null);
            if (!socket.isClosed()) socket.close();
            plugin.getPluginManager().callEvent(new SubNetworkDisconnectEvent());
            System.out.println("SubServers > The SubData Connection was closed");
            if (reconnect > 0) {
                System.out.println("SubServers > Attempting to reconnect in " + reconnect + " seconds");
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            plugin.subdata = new SubDataClient(plugin, name, socket.getInetAddress(), socket.getPort(), cipher);
                            timer.cancel();
                            while (queue.size() != 0) {
                                if (queue.get(0).name() != null) {
                                    plugin.subdata.forwardPacket(queue.get(0).get(), queue.get(0).name());
                                } else {
                                    plugin.subdata.sendPacket(queue.get(0).get());
                                }
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            System.out.println("SubServers > Connection was unsuccessful, retrying in " + reconnect + " seconds");
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
            }
        }
    }
}
