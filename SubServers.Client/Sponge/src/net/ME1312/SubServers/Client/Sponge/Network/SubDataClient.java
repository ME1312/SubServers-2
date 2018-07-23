package net.ME1312.SubServers.Client.Sponge.Network;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.ME1312.SubServers.Client.Sponge.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Client.Sponge.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Client.Sponge.Library.NamedContainer;
import net.ME1312.SubServers.Client.Sponge.Library.Util;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.Encryption.AES;
import net.ME1312.SubServers.Client.Sponge.Network.Packet.*;
import net.ME1312.SubServers.Client.Sponge.SubPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
    private static HashMap<Class<? extends PacketOut>, String> pOut = new HashMap<Class<? extends PacketOut>, String>();
    private static HashMap<String, List<PacketIn>> pIn = new HashMap<String, List<PacketIn>>();
    private static HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    private static boolean defaults = false;
    protected static Logger log;
    private PrintWriter writer;
    private NamedContainer<Boolean, Socket> socket;
    private String name;
    private Cipher cipher;
    private SubPlugin plugin;
    private LinkedList<NamedContainer<String, PacketOut>> queue;

    /**
     * SubServers Client Instance
     *
     * @param plugin SubPlugin
     * @param name Server Name
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
        this.queue = new LinkedList<NamedContainer<String, PacketOut>>();
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

        if (!defaults) loadDefaults();
        loop();

        sendPacket(new NamedContainer<>(null, new PacketAuthorization(plugin)));
    }

    private void init() {
        sendPacket(new PacketDownloadLang());
        while (queue.size() != 0) {
            sendPacket(queue.get(0));
            queue.remove(0);
        }
        socket.rename(true);
        Sponge.getEventManager().post(new SubNetworkConnectEvent(this));
    }

    static {
        addCipher("AES", new AES(128));
        addCipher("AES_128", new AES(128));
        addCipher("AES_192", new AES(192));
        addCipher("AES_256", new AES(256));
    } private void loadDefaults() {
        defaults = true;
        log = LoggerFactory.getLogger("SubData");

        registerPacket(new PacketAuthorization(plugin), "Authorization");
        registerPacket(new PacketCommandServer(), "SubCommandServer");
        registerPacket(new PacketCreateServer(), "SubCreateServer");
        registerPacket(new PacketDownloadHostInfo(), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubDownloadLang");
        registerPacket(new PacketDownloadNetworkList(), "SubDownloadNetworkList");
        registerPacket(new PacketDownloadPlayerList(), "SubDownloadPlayerList");
        registerPacket(new PacketDownloadProxyInfo(), "SubDownloadProxyInfo");
        registerPacket(new PacketDownloadServerInfo(), "SubDownloadServerInfo");
        registerPacket(new PacketDownloadServerList(), "SubDownloadServerList");
        registerPacket(new PacketInRunEvent(plugin), "SubRunEvent");
        registerPacket(new PacketInReload(plugin), "SubReload");
        registerPacket(new PacketInReset(), "SubReset");
        registerPacket(new PacketLinkServer(plugin), "SubLinkServer");
        registerPacket(new PacketStartServer(), "SubStartServer");
        registerPacket(new PacketStopServer(), "SubStopServer");

        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketCommandServer.class, "SubCommandServer");
        registerPacket(PacketCreateServer.class, "SubCreateServer");
        registerPacket(PacketDownloadHostInfo.class, "SubDownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubDownloadLang");
        registerPacket(PacketDownloadNetworkList.class, "SubDownloadNetworkList");
        registerPacket(PacketDownloadPlayerList.class, "SubDownloadPlayerList");
        registerPacket(PacketDownloadProxyInfo.class, "SubDownloadProxyInfo");
        registerPacket(PacketDownloadServerInfo.class, "SubDownloadServerInfo");
        registerPacket(PacketDownloadServerList.class, "SubDownloadServerList");
        registerPacket(PacketLinkServer.class, "SubLinkServer");
        registerPacket(PacketStartServer.class, "SubStartServer");
        registerPacket(PacketStopServer.class, "SubStopServer");
    }

    private void loop() {
        Sponge.getScheduler().createTaskBuilder().async().execute(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.get().getInputStream()));
                String input;
                while ((input = in.readLine()) != null) {
                    try {
                        YAMLSection data = cipher.decrypt(plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), Base64.getDecoder().decode(input));
                        for (PacketIn packet : decodePacket(data)) {
                            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                                try {
                                    packet.execute((data.contains("c")) ? data.getSection("c") : null);
                                } catch (Throwable e) {
                                    new InvocationTargetException(e, "Exception while executing PacketIn").printStackTrace();
                                }
                            }).submit(plugin);
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
        }).submit(plugin);
    }

    /**
     * Gets the Assigned Server Name
     *
     * @return Server Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Client Socket
     *
     * @return Client Socket
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
     * @param handle Handle to Bind
     */
    public static void registerPacket(PacketIn packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        List<PacketIn> list = (pIn.keySet().contains(handle.toLowerCase()))?pIn.get(handle.toLowerCase()):new ArrayList<PacketIn>();
        if (!list.contains(packet)) {
            list.add(packet);
            pIn.put(handle.toLowerCase(), list);
        }
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<String> search = new ArrayList<String>();
        search.addAll(pIn.keySet());
        for (String handle : search) if (pIn.get(handle.toLowerCase()).contains(packet)) {
            List<PacketIn> list = pIn.get(handle.toLowerCase());
            list.remove(packet);
            if (list.isEmpty()) {
                pIn.remove(handle.toLowerCase());
            } else {
                pIn.put(handle.toLowerCase(), list);
            }
        }
    }

    /**
     * Register PacketOut to the Network
     *
     * @param packet PacketOut to register
     * @param handle Handle to bind
     */
    public static void registerPacket(Class<? extends PacketOut> packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        pOut.put(packet, handle.toLowerCase());
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public static void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instances via handle
     *
     * @param handle Handle
     * @return PacketIn
     */
    public static List<? extends PacketIn> getPacket(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return new ArrayList<PacketIn>(pIn.get(handle.toLowerCase()));
    }

    /**
     * Send Packet to Server
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (socket == null || !socket.name()) {
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
            sendPacket(new NamedContainer<>(location, packet));
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
        YAMLSection data = new YAMLSection();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException("Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException("PacketOut Version cannot be null: " + packet.getClass().getCanonicalName());

        try {
            YAMLSection contents = packet.generate();
            data.set("h", pOut.get(packet.getClass()));
            data.set("v", packet.getVersion().toString());
            if (contents != null) data.set("c", contents);
            return data;
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
        if (!pIn.keySet().contains(data.getRawString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + data.getRawString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(data.getRawString("h"))) {
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
            Sponge.getEventManager().post(new SubNetworkDisconnectEvent());
            log.info("The SubData Connection was closed");
            if (reconnect > 0) {

                log.info("SubServers > Attempting to reconnect in " + reconnect + " seconds");
                Sponge.getScheduler().createTaskBuilder().async().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            plugin.subdata = new SubDataClient(plugin, name, socket.getInetAddress(), socket.getPort(), cipher);
                            while (queue.size() != 0) {
                                if (queue.get(0).name() != null) {
                                    plugin.subdata.forwardPacket(queue.get(0).get(), queue.get(0).name());
                                } else {
                                    plugin.subdata.sendPacket(queue.get(0).get());
                                }
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            log.info("SubServers > Connection was unsuccessful, retrying in " + reconnect + " seconds");
                            Sponge.getScheduler().createTaskBuilder().async().execute(this).delay(reconnect, TimeUnit.SECONDS).submit(plugin);
                        }
                    }
                }).delay(reconnect, TimeUnit.SECONDS).submit(plugin);
            }
        }
    }
}
