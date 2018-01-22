package net.ME1312.SubServers.Bungee.Network;

import net.ME1312.SubServers.Bungee.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Encryption.AES;
import net.ME1312.SubServers.Bungee.Network.Packet.*;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubDataServer Class
 */
public final class SubDataServer {
    private static int MAX_QUEUE = 64;
    private static HashMap<Class<? extends PacketOut>, String> pOut = new HashMap<Class<? extends PacketOut>, String>();
    private static HashMap<String, List<PacketIn>> pIn = new HashMap<String, List<PacketIn>>();
    private static HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    private static List<String> allowedAddresses = new ArrayList<String>();
    private static boolean defaults = false;
    private HashMap<String, Client> clients = new HashMap<String, Client>();
    private ServerSocket server;
    private Cipher cipher;
    protected SubPlugin plugin;

    /**
     * SubData Server Instance
     *
     * @param plugin SubPlugin
     * @param port Port
     * @param address Bind
     * @param cipher Cipher (or null for none)
     * @throws IOException
     */
    public SubDataServer(SubPlugin plugin, int port, InetAddress address, Cipher cipher) throws IOException {
        if (Util.isNull(plugin, port, MAX_QUEUE)) throw new NullPointerException();
        if (address == null) {
            server = new ServerSocket(port, MAX_QUEUE);
            allowConnection("127.0.0.1");
        } else {
            server = new ServerSocket(port, MAX_QUEUE, address);
            allowConnection(address.getHostAddress());
        }
        this.plugin = plugin;
        this.cipher = (cipher != null)?cipher:new Cipher() {
            @Override
            public String getName() {
                return "NONE";
            }
            @Override
            public byte[] encrypt(String key, JSONObject data) throws Exception {
                return data.toString().getBytes(StandardCharsets.UTF_8);
            }
            @Override
            public JSONObject decrypt(String key, byte[] data) throws Exception {
                return new JSONObject(new String(data, StandardCharsets.UTF_8));
            }
        };

        if (!defaults) loadDefaults();
    }

    static {
        addCipher("AES", new AES(128));
        addCipher("AES_128", new AES(128));
        addCipher("AES_192", new AES(192));
        addCipher("AES_256", new AES(256));
    } private void loadDefaults() {
        defaults = true;

        plugin.getPluginManager().registerListener(null, new PacketOutRunEvent(plugin));

        registerPacket(new PacketAuthorization(plugin), "Authorization");
        registerPacket(new PacketCommandServer(plugin), "SubCommandServer");
        registerPacket(new PacketCreateServer(plugin), "SubCreateServer");
        registerPacket(new PacketDownloadHostInfo(plugin), "SubDownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubDownloadLang");
        registerPacket(new PacketDownloadNetworkList(plugin), "SubDownloadNetworkList");
        registerPacket(new PacketDownloadPlayerList(plugin), "SubDownloadPlayerList");
        registerPacket(new PacketDownloadProxyInfo(plugin), "SubDownloadProxyInfo");
        registerPacket(new PacketDownloadServerInfo(plugin), "SubDownloadServerInfo");
        registerPacket(new PacketDownloadServerList(plugin), "SubDownloadServerList");
        registerPacket(new PacketEditServer(plugin), "SubEditServer");
        registerPacket(new PacketExAddServer(), "SubExAddServer");
        registerPacket(new PacketExConfigureHost(plugin), "SubExConfigureHost");
        registerPacket(new PacketExCreateServer(null), "SubExCreateServer");
        registerPacket(new PacketExDeleteServer(), "SubExDeleteServer");
        registerPacket(new PacketExRemoveServer(), "SubExRemoveServer");
        registerPacket(new PacketExUpdateServer(plugin), "SubExUpdateServer");
        registerPacket(new PacketInExLogMessage(), "SubExLogMessage");
        registerPacket(new PacketInExRequestQueue(plugin), "SubExRequestQueue");
        registerPacket(new PacketLinkExHost(plugin), "SubLinkExHost");
        registerPacket(new PacketLinkProxy(plugin), "SubLinkProxy");
        registerPacket(new PacketLinkServer(plugin), "SubLinkServer");
        registerPacket(new PacketListenLog(plugin), "SubListenLog");
        registerPacket(new PacketStartServer(plugin), "SubStartServer");
        registerPacket(new PacketStopServer(plugin), "SubStopServer");

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
        registerPacket(PacketEditServer.class, "SubEditServer");
        registerPacket(PacketExAddServer.class, "SubExAddServer");
        registerPacket(PacketExConfigureHost.class, "SubExConfigureHost");
        registerPacket(PacketExCreateServer.class, "SubExCreateServer");
        registerPacket(PacketExDeleteServer.class, "SubExDeleteServer");
        registerPacket(PacketExRemoveServer.class, "SubExRemoveServer");
        registerPacket(PacketExUpdateServer.class, "SubExUpdateServer");
        registerPacket(PacketLinkExHost.class, "SubLinkExHost");
        registerPacket(PacketLinkProxy.class, "SubLinkProxy");
        registerPacket(PacketLinkServer.class, "SubLinkServer");
        registerPacket(PacketListenLog.class, "SubListenLog");
        registerPacket(PacketOutRunEvent.class, "SubRunEvent");
        registerPacket(PacketOutReload.class, "SubReload");
        registerPacket(PacketOutReset.class, "SubReset");
        registerPacket(PacketStartServer.class, "SubStartServer");
        registerPacket(PacketStopServer.class, "SubStopServer");
    }

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public ServerSocket getServer() {
        return server;
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
     * Gets the Server's Cipher
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
     * Add a Client to the Network
     *
     * @param socket Client to add
     * @throws IOException
     */
    public Client addClient(Socket socket) throws IOException {
        if (Util.isNull(socket)) throw new NullPointerException();
        if (checkConnection(socket.getInetAddress())) {
            Client client = new Client(this, socket);
            System.out.println("SubData > " + client.getAddress().toString() + " has connected");
            clients.put(client.getAddress().toString(), client);
            return client;
        } else {
            System.out.println("SubData > " + socket.getInetAddress().toString() + " attempted to connect, but isn't white-listed");
            socket.close();
            return null;
        }
    }

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public Client getClient(Socket socket) {
        if (Util.isNull(socket)) throw new NullPointerException();
        return clients.get(new InetSocketAddress(socket.getInetAddress(), socket.getPort()).toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(InetSocketAddress address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address.toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address);
    }

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public Collection<Client> getClients() {
        return clients.values();
    }

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public void removeClient(Client client) throws IOException {
        if (Util.isNull(client)) throw new NullPointerException();
        SocketAddress address = client.getAddress();
        if (clients.keySet().contains(address.toString())) {
            clients.remove(address.toString());
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(InetSocketAddress address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        Client client = clients.get(address.toString());
        if (clients.keySet().contains(address.toString())) {
            clients.remove(address.toString());
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(String address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        Client client = clients.get(address);
        if (clients.keySet().contains(address)) {
            clients.remove(address);
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
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
     * Broadcast a Packet to everything on the Network<br>
     * <b>Warning:</b> There are usually different types of applications on the network at once, they may not recognise the same packet handles
     *
     * @param packet Packet to send
     */
    public void broadcastPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<Client> clients = new ArrayList<Client>();
        clients.addAll(getClients());
        for (Client client : clients) {
            client.sendPacket(packet);
        }
    }

    /**
     * Allow Connections from an Address
     *
     * @param address Address to allow
     */
    public static void allowConnection(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        if (!allowedAddresses.contains(address)) allowedAddresses.add(address);
    }

    private boolean checkConnection(InetAddress address) {
        boolean whitelisted = false;
        Matcher regaddress = Pattern.compile("^(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})$").matcher(address.getHostAddress());
        if (regaddress.find()) {
            for (String allowed : allowedAddresses) if (!whitelisted) {
                Matcher regallowed = Pattern.compile("^(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%)$").matcher(allowed);
                if (regallowed.find() && (
                        (regaddress.group(1).equals(regallowed.group(1)) || regallowed.group(1).equals("%")) &&
                        (regaddress.group(2).equals(regallowed.group(2)) || regallowed.group(2).equals("%")) &&
                        (regaddress.group(3).equals(regallowed.group(3)) || regallowed.group(3).equals("%")) &&
                        (regaddress.group(4).equals(regallowed.group(4)) || regallowed.group(4).equals("%"))
                        )) whitelisted = true;
            }
        }
        return whitelisted;
    }

    /**
     * Deny Connections from an Address
     *
     * @param address Address to deny
     */
    public static void denyConnection(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        allowedAddresses.remove(address);
    }

    /**
     * JSON Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    protected static JSONObject encodePacket(Client client, PacketOut packet) throws IllegalPacketException, InvocationTargetException {
        JSONObject json = new JSONObject();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException(packet.getClass().getCanonicalName() + ": Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException(packet.getClass().getCanonicalName() + ": PacketOut getVersion() cannot be null: " + packet.getClass().getCanonicalName());

        try {
            JSONObject contents = packet.generate();
            json.put("h", pOut.get(packet.getClass()));
            json.put("v", packet.getVersion().toString());
            if (contents != null) json.put("c", contents);
            return json;
        } catch (Throwable e) {
            throw new InvocationTargetException(e, packet.getClass().getCanonicalName() + ": Exception while encoding packet");
        }
    }

    /**
     * JSON Decode PacketIn
     *
     * @param json JSON to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     */
    protected static List<PacketIn> decodePacket(Client client, JSONObject json) throws IllegalPacketException {
        if (!json.keySet().contains("h") || !json.keySet().contains("v")) throw new IllegalPacketException(client.getAddress().toString() + ": Unknown Packet Format: " + json.toString());
        if (!pIn.keySet().contains(json.getString("h"))) throw new IllegalPacketException(client.getAddress().toString() + ": Unknown PacketIn Channel: " + json.getString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(json.getString("h"))) {
            if (packet.isCompatible(new Version(json.getString("v")))) {
                list.add(packet);
            } else {
                new IllegalPacketException(client.getAddress().toString() + ": Packet Version Mismatch in " + json.getString("h") + ": " + json.getString("v") + " =/= " + packet.getVersion().toString()).printStackTrace();
            }
        }

        return list;
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy() throws IOException {
        while(clients.size() > 0) {
            removeClient((Client) clients.values().toArray()[0]);
        }
        server.close();
        System.out.println("SubServers > The SubData Listener has been closed");
        plugin.subdata = null;
    }
}
