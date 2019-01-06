package net.ME1312.SubServers.Host.Network;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Host.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Host.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Host.Network.Encryption.AES;
import net.ME1312.SubServers.Host.Network.Packet.*;
import net.ME1312.SubServers.Host.SubAPI;
import net.ME1312.SubServers.Host.ExHost;
import org.json.JSONException;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
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
    protected static Logger log;
    private MessagePacker out;
    private NamedContainer<Boolean, Socket> socket;
    private String name;
    private Cipher cipher;
    private ExHost host;
    private LinkedList<NamedContainer<String, PacketOut>> queue;

    /**
     * SubServers Client Instance
     *
     * @param host SubServers.Host
     * @param name Name of Host
     * @param address Address
     * @param port Port
     * @param cipher Cipher
     * @throws IOException
     */
    public SubDataClient(ExHost host, String name, InetAddress address, int port, Cipher cipher) throws IOException {
        if (Util.isNull(host, name, address, port)) throw new NullPointerException();
        socket = new NamedContainer<>(false, new Socket(address, port));
        this.host = host;
        this.name = name;
        this.out = MessagePack.newDefaultPacker(socket.get().getOutputStream());
        this.queue = new LinkedList<NamedContainer<String, PacketOut>>();
        this.cipher = (cipher != null)?cipher:new Cipher() {
            @Override
            public String getName() {
                return "NONE";
            }
            @Override
            public Value encrypt(String key, YAMLSection data) {
                return convert(data);
            }
            @Override
            @SuppressWarnings("unchecked")
            public YAMLSection decrypt(String key, Value data) {
                return convert(data.asMapValue());
            }
        };

        if (!defaults) loadDefaults();
        loop();

        sendPacket(new NamedContainer<>(null, new PacketAuthorization(host)));
    }

    private void init() {
        sendPacket(new PacketExConfigureHost(host));
        sendPacket(new PacketDownloadLang());
        sendPacket(new PacketOutExRequestQueue());
        while (queue.size() != 0) {
            sendPacket(queue.get(0));
            queue.remove(0);
        }
        socket.rename(true);
        GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubNetworkConnectEvent(host.subdata));
    }

    static {
        addCipher("AES", new AES(128));
        addCipher("AES_128", new AES(128));
        addCipher("AES_192", new AES(192));
        addCipher("AES_256", new AES(256));
    } private void loadDefaults() {
        defaults = true;
        log = new Logger("SubData");

        registerPacket(new PacketAuthorization(host), "SubData", "Authorization");
        registerPacket(new PacketCommandServer(), "SubServers", "CommandServer");
        registerPacket(new PacketCreateServer(), "SubServers", "CreateServer");
        registerPacket(new PacketDownloadGroupInfo(), "SubServers", "DownloadGroupInfo");
        registerPacket(new PacketDownloadHostInfo(), "SubServers", "DownloadHostInfo");
        registerPacket(new PacketDownloadLang(host), "SubServers", "DownloadLang");
        registerPacket(new PacketDownloadNetworkList(), "SubServers", "DownloadNetworkList");
        registerPacket(new PacketDownloadPlatformInfo(), "SubServers", "DownloadPlatformInfo");
        registerPacket(new PacketDownloadPlayerList(), "SubServers", "DownloadPlayerList");
        registerPacket(new PacketDownloadProxyInfo(), "SubServers", "DownloadProxyInfo");
        registerPacket(new PacketDownloadServerInfo(), "SubServers", "DownloadServerInfo");
        registerPacket(new PacketExAddServer(host), "SubServers", "ExAddServer");
        registerPacket(new PacketExConfigureHost(host), "SubServers", "ExConfigureHost");
        registerPacket(new PacketExCreateServer(host), "SubServers", "ExCreateServer");
        registerPacket(new PacketExDeleteServer(host), "SubServers", "ExDeleteServer");
        registerPacket(new PacketExRemoveServer(host), "SubServers", "ExRemoveServer");
        registerPacket(new PacketExUpdateServer(host), "SubServers", "ExUpdateServer");
        registerPacket(new PacketInReload(host), "SubServers", "Reload");
        registerPacket(new PacketInReset(host), "SubServers", "Reset");
        registerPacket(new PacketInRunEvent(), "SubServers", "RunEvent");
        registerPacket(new PacketLinkExHost(host), "SubServers", "LinkExHost");
        registerPacket(new PacketStartServer(), "SubServers", "StartServer");
        registerPacket(new PacketStopServer(), "SubServers", "StopServer");


        registerPacket(PacketAuthorization.class, "SubData", "Authorization");
        registerPacket(PacketCommandServer.class, "SubServers", "CommandServer");
        registerPacket(PacketCreateServer.class, "SubServers", "CreateServer");
        registerPacket(PacketDownloadGroupInfo.class, "SubServers", "DownloadGroupInfo");
        registerPacket(PacketDownloadHostInfo.class, "SubServers", "DownloadHostInfo");
        registerPacket(PacketDownloadLang.class, "SubServers", "DownloadLang");
        registerPacket(PacketDownloadNetworkList.class, "SubServers", "DownloadNetworkList");
        registerPacket(PacketDownloadPlatformInfo.class, "SubServers", "DownloadPlatformInfo");
        registerPacket(PacketDownloadPlayerList.class, "SubServers", "DownloadPlayerList");
        registerPacket(PacketDownloadProxyInfo.class, "SubServers", "DownloadProxyInfo");
        registerPacket(PacketDownloadServerInfo.class, "SubServers", "DownloadServerInfo");
        registerPacket(PacketExAddServer.class, "SubServers", "ExAddServer");
        registerPacket(PacketExConfigureHost.class, "SubServers", "ExConfigureHost");
        registerPacket(PacketExCreateServer.class, "SubServers", "ExCreateServer");
        registerPacket(PacketExDeleteServer.class, "SubServers", "ExDeleteServer");
        registerPacket(PacketExRemoveServer.class, "SubServers", "ExRemoveServer");
        registerPacket(PacketExUpdateServer.class, "SubServers", "ExUpdateServer");
        registerPacket(PacketLinkExHost.class, "SubServers", "LinkExHost");
        registerPacket(PacketOutExLogMessage.class, "SubServers", "ExLogMessage");
        registerPacket(PacketOutExRequestQueue.class, "SubServers", "ExRequestQueue");
        registerPacket(PacketStartServer.class, "SubServers", "StartServer");
        registerPacket(PacketStopServer.class, "SubServers", "StopServer");
    }

    private void loop() {
        new Thread(() -> {
            try {
                MessageUnpacker in = MessagePack.newDefaultUnpacker(socket.get().getInputStream());
                Value input;
                while ((input = in.unpackValue()) != null) {
                    recieve(input);
                }
                try {
                    destroy(host.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    log.error.println(e1);
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException || e instanceof MessageInsufficientBufferException)) log.error.println(e);
                try {
                    destroy(host.config.get().getSection("Settings").getSection("SubData").getInt("Reconnect", 30));
                } catch (IOException e1) {
                    log.error.println(e1);
                }
            }
        }, SubAPI.getInstance().getAppInfo().getName() + "::SubData_Packet_Listener").start();
    }

    private void recieve(Value input) {
        try {
            YAMLSection data = cipher.decrypt(host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), input);
            for (PacketIn packet : decodePacket(data)) {
                try {
                    packet.execute((data.contains("c"))?data.getSection("c"):null);
                } catch (Throwable e) {
                    log.error.println(new InvocationTargetException(e, "Exception while executing PacketIn"));
                }
            }
        } catch (JSONException | YAMLException e) {
            log.error.println(new IllegalPacketException("Unknown Packet Format: " + input));
        } catch (IllegalPacketException e) {
            log.error.println(e);
        } catch (Exception e) {
            log.error.println(new InvocationTargetException(e, "Exception while decoding packet"));
        }
    }

    /**
     * Gets the Assigned Host Name
     *
     * @return Host Name
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
     * Send Packet to Client
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
            out.packValue(getCipher().encrypt(host.config.get().getSection("Settings").getSection("SubData").getRawString("Password"), data));
            out.flush();
        } catch (Throwable e) {
            log.error.println(e);
        }
    }
    /**
     * Forward Packet to Server
     *
     * @param packet Packet to send
     * @param location Where to send
     */
    public void forwardPacket(PacketOut packet, String location) {
        if (Util.isNull(packet, location)) throw new NullPointerException();
        if (socket == null || !socket.name()) {
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
     * Convert a YAMLSection to a MessagePack Map
     *
     * @param config YAMLSection
     * @return MessagePack Map
     */
    public static MapValue convert(YAMLSection config) {
        return (MapValue) msgPack(config.get());
    }
    @SuppressWarnings("unchecked")
    private static Value msgPack(Object value) {
        if (value == null) {
            return ValueFactory.newNil();
        } else if (value instanceof Value) {
            return (Value) value;
        } else if (value instanceof Map) {
            ValueFactory.MapBuilder map = ValueFactory.newMapBuilder();
            for (String key : ((Map<String, ?>) value).keySet()) {
                Value v = msgPack(((Map<String, ?>) value).get(key));
                if (v != null) map.put(ValueFactory.newString(key), v);
            }
            return map.build();
        } else if (value instanceof Collection) {
            LinkedList<Value> values = new LinkedList<Value>();
            for (Object object : (Collection<?>) value) {
                Value v = msgPack(object);
                if (v != null) values.add(v);
            }
            return ValueFactory.newArray(values);
        } else if (value instanceof Boolean) {
            return ValueFactory.newBoolean((boolean) value);
        } else if (value instanceof Number) {
            if (((Number) value).doubleValue() == (double)(int) ((Number) value).doubleValue()) {
                return ValueFactory.newInteger(((Number) value).longValue());
            } else {
                return ValueFactory.newFloat(((Number) value).doubleValue());
            }
        } else if (value instanceof String) {
            return ValueFactory.newString((String) value);
        } else {
            return null;
        }
    }

    /**
     * Convert a MessagePack Map to a YAMLSection
     *
     * @param msgpack MessagePack Map
     * @return YAMLSection
     */
    @SuppressWarnings("unchecked")
    public static YAMLSection convert(MapValue msgpack) {
        YAMLSection section = new YAMLSection();

        boolean warned = false;
        Map<Value, Value> map = msgpack.map();
        for (Value key : map.keySet()) {
            if (key.isStringValue()) {
                section.set(key.asStringValue().asString(), simplify(map.get(key)));
            } else if (!warned) {
                new IllegalStateException("MessagePack contains non-string key(s)").printStackTrace();
                warned = true;
            }
        }

        return section;
    }
    private static Object simplify(Value value) {
        Object simple = value;
        if (value.isNilValue()) {
            simple = null;
        } else if (value.isMapValue()) {
            Map<Value, Value> map = value.asMapValue().map();
            simple = convert(value.asMapValue());
        } else if (value.isArrayValue()) {
            simple = value.asArrayValue().list();
        } else if (value.isBooleanValue()) {
            simple = value.asBooleanValue().getBoolean();
        } else if (value.isFloatValue()) {
            if (value.asFloatValue().toDouble() == (double)(float) value.asFloatValue().toDouble()) {
                simple = value.asFloatValue().toFloat();
            } else {
                simple = value.asFloatValue().toDouble();
            }
        } else if (value.isIntegerValue()) {
            if (value.asIntegerValue().isInByteRange()) {
                simple = value.asIntegerValue().asByte();
            } else if (value.asIntegerValue().isInShortRange()) {
                simple = value.asIntegerValue().asShort();
            } else if (value.asIntegerValue().isInIntRange()) {
                simple = value.asIntegerValue().asInt();
            } else if (value.asIntegerValue().isInLongRange()) {
                simple = value.asIntegerValue().asLong();
            } else {
                simple = value.asIntegerValue().asBigInteger();
            }
        } else if (value.isStringValue()) {
            simple = value.asStringValue().asString();
        }

        return simple;
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
        } catch (Throwable e) {
            throw new InvocationTargetException(e, "Exception while encoding packet");
        }
        return json;
    }

    /**
     * Decode PacketIn
     *
     * @param data Data to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("deprecation")
    private static List<PacketIn> decodePacket(YAMLSection data) throws IllegalPacketException, InvocationTargetException {
        if (!data.contains("n") || !data.contains("h") || !data.contains("v")) throw new IllegalPacketException("Unknown Packet Format: " + data.toString());
        if (!pIn.keySet().contains(data.getRawString("n")) || !pIn.get(data.getRawString("n")).keySet().contains(data.getRawString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + data.getRawString("n") + ':' + data.getRawString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(data.getRawString("n")).get(data.getRawString("h"))) {
            if (packet.isCompatible(new Version(data.getRawString("v")))) {
                list.add(packet);
            } else {
                SubAPI.getInstance().getInternals().log.error.println(new IllegalPacketException("Packet Version Mismatch in " + data.getRawString("h") + ": " + data.getRawString("v") + " -> " + packet.getVersion().toString()));
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
            GalaxiEngine.getInstance().getPluginManager().executeEvent(new SubNetworkDisconnectEvent());
            log.info.println("The SubData Connection was closed");
            if (reconnect > 0) {
                log.info.println("Attempting to reconnect in " + reconnect + " seconds");
                Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::SubData_Reconnect_Handler");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            host.subdata = new SubDataClient(host, name, socket.getInetAddress(), socket.getPort(), cipher);
                            timer.cancel();
                            while (queue.size() != 0) {
                                if (queue.get(0).name() != null) {
                                    host.subdata.forwardPacket(queue.get(0).get(), queue.get(0).name());
                                } else {
                                    host.subdata.sendPacket(queue.get(0).get());
                                }
                                queue.remove(0);
                            }
                        } catch (IOException e) {
                            log.warn.println("Connection was unsuccessful, retrying in " + reconnect + " seconds");
                        }
                    }
                }, TimeUnit.SECONDS.toMillis(reconnect), TimeUnit.SECONDS.toMillis(reconnect));
            }
        }
    }
}
