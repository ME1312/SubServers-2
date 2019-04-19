package net.ME1312.SubServers.Bungee.Network;

import com.dosse.upnp.UPnP;
import net.ME1312.SubServers.Bungee.Event.SubNetworkConnectEvent;
import net.ME1312.SubServers.Bungee.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.IllegalPacketException;
import net.ME1312.SubServers.Bungee.Library.NamedContainer;
import net.ME1312.SubServers.Bungee.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Encryption.AES;
import net.ME1312.SubServers.Bungee.Network.Packet.*;
import net.ME1312.SubServers.Bungee.SubPlugin;
import org.msgpack.value.Value;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubDataServer Class
 */
public final class SubDataServer {
    private static int MAX_QUEUE = 64;
    private static HashMap<Class<? extends PacketOut>, NamedContainer<String, String>> pOut = new HashMap<Class<? extends PacketOut>, NamedContainer<String, String>>();
    private static HashMap<String, HashMap<String, List<PacketIn>>> pIn = new HashMap<String, HashMap<String, List<PacketIn>>>();
    private static HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    private static List<String> allowedAddresses = new ArrayList<String>();
    private static boolean defaults = false;
    private HashMap<String, Client> clients = new HashMap<String, Client>();
    private ServerSocket server;
    private Cipher cipher;
    protected SubPlugin plugin;
    String password;

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
        if (UPnP.isUPnPAvailable() && plugin.config.get().getSection("Settings").getSection("UPnP", new YAMLSection()).getBoolean("Forward-SubData", false)) UPnP.openPortTCP(port);
        this.plugin = plugin;
        this.password = plugin.config.get().getSection("Settings").getSection("SubData").getRawString("Password");
        this.cipher = (cipher != null)?cipher:new Cipher() {
            @Override
            public String getName() {
                return "NONE";
            }
            @Override
            public Value encrypt(String key, YAMLSection data) {
                return data.msgPack();
            }
            @Override
            @SuppressWarnings("unchecked")
            public YAMLSection decrypt(String key, Value data) {
                return new YAMLSection(data.asMapValue());
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

        registerPacket(new PacketAuthorization(plugin), "SubData", "Authorization");
        registerPacket(new PacketCommandServer(plugin), "SubServers", "CommandServer");
        registerPacket(new PacketCreateServer(plugin), "SubServers", "CreateServer");
        registerPacket(new PacketDownloadGroupInfo(plugin), "SubServers", "DownloadGroupInfo");
        registerPacket(new PacketDownloadHostInfo(plugin), "SubServers", "DownloadHostInfo");
        registerPacket(new PacketDownloadLang(plugin), "SubServers", "DownloadLang");
        registerPacket(new PacketDownloadNetworkList(plugin), "SubServers", "DownloadNetworkList");
        registerPacket(new PacketDownloadPlatformInfo(plugin), "SubServers", "DownloadPlatformInfo");
        registerPacket(new PacketDownloadPlayerList(plugin), "SubServers", "DownloadPlayerList");
        registerPacket(new PacketDownloadProxyInfo(plugin), "SubServers", "DownloadProxyInfo");
        registerPacket(new PacketDownloadServerInfo(plugin), "SubServers", "DownloadServerInfo");
        registerPacket(new PacketExAddServer(), "SubServers", "ExAddServer");
        registerPacket(new PacketExConfigureHost(plugin), "SubServers", "ExConfigureHost");
        registerPacket(new PacketExCreateServer(null), "SubServers", "ExCreateServer");
        registerPacket(new PacketExDeleteServer(), "SubServers", "ExDeleteServer");
        registerPacket(new PacketExRemoveServer(), "SubServers", "ExRemoveServer");
        registerPacket(new PacketExUpdateServer(plugin), "SubServers", "ExUpdateServer");
        registerPacket(new PacketInExLogMessage(), "SubServers", "ExLogMessage");
        registerPacket(new PacketInExRequestQueue(plugin), "SubServers", "ExRequestQueue");
        registerPacket(new PacketLinkExHost(plugin), "SubServers", "LinkExHost");
        registerPacket(new PacketLinkProxy(plugin), "SubServers", "LinkProxy");
        registerPacket(new PacketLinkServer(plugin), "SubServers", "LinkServer");
        registerPacket(new PacketRestartServer(plugin), "SubServers", "RestartServer");
        registerPacket(new PacketStartServer(plugin), "SubServers", "StartServer");
        registerPacket(new PacketStopServer(plugin), "SubServers", "StopServer");

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
        registerPacket(PacketOutExUpdateWhitelist.class, "SubServers", "ExUpdateWhitelist");
        registerPacket(PacketLinkExHost.class, "SubServers", "LinkExHost");
        registerPacket(PacketLinkProxy.class, "SubServers", "LinkProxy");
        registerPacket(PacketLinkServer.class, "SubServers", "LinkServer");
        registerPacket(PacketOutRunEvent.class, "SubServers", "RunEvent");
        registerPacket(PacketOutReload.class, "SubServers", "Reload");
        registerPacket(PacketOutReset.class, "SubServers", "Reset");
        registerPacket(PacketRestartServer.class, "SubServers", "RestartServer");
        registerPacket(PacketStartServer.class, "SubServers", "StartServer");
        registerPacket(PacketStopServer.class, "SubServers", "StopServer");
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
        return getClient(new InetSocketAddress(socket.getInetAddress(), socket.getPort()));
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(InetSocketAddress address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return getClient(address.toString());
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
        removeClient(client.getAddress());
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(InetSocketAddress address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        removeClient(address.toString());
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(String address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        if (clients.keySet().contains(address)) {
            Client client = clients.get(address);
            plugin.getPluginManager().callEvent(new SubNetworkDisconnectEvent(this, client));
            clients.remove(address);
            client.disconnect();
            System.out.println("SubData > " + client.getAddress().toString() + " has disconnected");
        }
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
        SubNetworkConnectEvent event = new SubNetworkConnectEvent(this, address);
        event.setCancelled(!whitelisted);
        plugin.getPluginManager().callEvent(event);
        return !event.isCancelled();
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
     * Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    protected static YAMLSection encodePacket(Client client, PacketOut packet) throws IllegalPacketException, InvocationTargetException {
        YAMLSection section = new YAMLSection();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException(packet.getClass().getCanonicalName() + ": Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion() == null) throw new NullPointerException(packet.getClass().getCanonicalName() + ": PacketOut getVersion() cannot be null: " + packet.getClass().getCanonicalName());

        try {
            YAMLSection contents = packet.generate();
            section.set("n", pOut.get(packet.getClass()).name());
            section.set("h", pOut.get(packet.getClass()).get());
            section.set("v", packet.getVersion());
            if (contents != null) section.set("c", contents);
            return section;
        } catch (Throwable e) {
            throw new InvocationTargetException(e, packet.getClass().getCanonicalName() + ": Exception while encoding packet");
        }
    }

    /**
     * Decode PacketIn
     *
     * @param data Data to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     */
    protected static List<PacketIn> decodePacket(Client client, YAMLSection data) throws IllegalPacketException {
        if (!data.contains("n") || !data.contains("h") || !data.contains("v")) throw new IllegalPacketException(client.getAddress().toString() + ": Unknown Packet Format: " + data.toString());
        if (!pIn.keySet().contains(data.getRawString("n")) || !pIn.get(data.getRawString("n")).keySet().contains(data.getRawString("h"))) throw new IllegalPacketException("Unknown PacketIn Channel: " + data.getRawString("n") + ':' + data.getRawString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(data.getRawString("n")).get(data.getRawString("h"))) {
            if (packet.isCompatible(data.getVersion("v"))) {
                list.add(packet);
            } else {
                new IllegalPacketException(client.getAddress().toString() + ": Packet Version Mismatch in " + data.getRawString("h") + ": " + data.getRawString("v") + " =/= " + packet.getVersion().toFullString()).printStackTrace();
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
        if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(server.getLocalPort())) UPnP.closePortTCP(server.getLocalPort());
        System.out.println("SubServers > The SubData Listener has been closed");
        plugin.subdata = null;
    }
}
